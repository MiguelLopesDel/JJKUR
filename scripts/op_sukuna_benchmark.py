#!/usr/bin/env python3
import argparse
import json
import os
import re
import signal
import subprocess
import sys
import time
from collections import Counter, defaultdict
from pathlib import Path
from tempfile import NamedTemporaryFile


READY_PATTERNS = (
    "Done (",
    "For help, type",
)


def parse_args():
    parser = argparse.ArgumentParser(description="Run OP Sukuna AI benchmark on a Forge runServer.")
    parser.add_argument("--scenario", default="itadori_modulo", choices=["itadori_modulo"])
    parser.add_argument("--fights", type=int, default=100)
    parser.add_argument("--duration", type=int, default=2400, help="Max duration per fight in ticks.")
    parser.add_argument("--parallel", default="auto", help="Arena count, auto, or max.")
    parser.add_argument("--max-parallel", type=int, default=8, help="Upper limit used by --parallel auto/max.")
    parser.add_argument("--arena-spacing", type=int, default=1024, help="Distance in blocks between benchmark arenas.")
    parser.add_argument("--arena-starts-per-tick", type=int, default=1, help="How many new arenas may be prepared/started in one server tick.")
    parser.add_argument("--variant-set", default="standard")
    parser.add_argument("--repo", default=".", help="Repository root.")
    parser.add_argument("--ready-timeout", type=int, default=180)
    parser.add_argument("--benchmark-timeout", type=int, default=2400, help="Seconds to wait for each benchmark phase (default: 2400 / 40min, 0=auto).")
    parser.add_argument("--benchmark-stop-timeout", type=int, default=180, help="Seconds to wait for forced partial export after a benchmark timeout.")
    parser.add_argument("--stop-server", dest="stop_server", action="store_true", default=True, help="Send stop to the server after the benchmark finishes.")
    parser.add_argument("--no-stop-server", "--keep-server", dest="stop_server", action="store_false", help="Keep the server open after the benchmark finishes.")
    parser.add_argument("--telemetry-profile", choices=["full", "balanced", "minimal"], default="balanced")
    parser.add_argument("--sample-interval", type=int, default=0, help="Benchmark sample interval ticks (0=auto by telemetry profile).")
    parser.add_argument("--two-phase", dest="two_phase", action="store_true", default=True, help="Run phase1 quick gate then phase2 full run.")
    parser.add_argument("--single-phase", dest="two_phase", action="store_false", help="Run only one phase.")
    parser.add_argument("--phase1-fights", type=int, default=40)
    parser.add_argument("--phase1-duration", type=int, default=2400)
    parser.add_argument("--phase1-gate", type=float, default=0.55, help="Minimum win rate in phase1 to unlock phase2.")
    parser.add_argument("--auto-parallel", dest="auto_parallel", action="store_true", default=True, help="Auto-tune parallel arenas for stability.")
    parser.add_argument("--no-auto-parallel", dest="auto_parallel", action="store_false", help="Disable auto-tuned parallel heuristic.")
    parser.add_argument("--parallel-probe", action="store_true", help="Probe multiple parallel values with short runs (slow, optional).")
    parser.add_argument("--parallel-probe-values", default="6,8,10,12,14,16")
    parser.add_argument("--parallel-probe-fights", type=int, default=12)
    parser.add_argument("--parallel-probe-duration", type=int, default=1000)
    parser.add_argument("--analyze-only", default="", help="Only postprocess an existing benchmark directory; do not run server.")
    parser.add_argument("--benchmark-id", default="", help="Benchmark id override for --analyze-only.")
    return parser.parse_args()


def parallel_value(value, max_parallel):
    cap = max(1, min(max_parallel, 32))
    if value == "max":
        return cap
    if value != "auto":
        return max(1, min(int(value), cap))
    cpu = os.cpu_count() or 2
    return max(1, min(max(1, cpu - 2), cap))


def sample_interval_for_profile(profile):
    if profile == "full":
        return 5
    if profile == "minimal":
        return 20
    return 10


def effective_parallel(value, max_parallel, auto_parallel, telemetry_profile, fights):
    if not auto_parallel:
        return parallel_value(value, max_parallel)
    if value not in ("auto", "max"):
        return parallel_value(value, max_parallel)
    cpu = os.cpu_count() or 4
    cap = max(1, min(max_parallel, 32))
    baseline = max(2, cpu - 2)
    if telemetry_profile == "minimal":
        baseline += 2
    if fights >= 100:
        baseline = min(baseline, 16)
    elif fights >= 60:
        baseline = min(baseline, 18)
    else:
        baseline = min(baseline, 20)
    return max(1, min(cap, baseline))


def auto_benchmark_timeout(fights, duration_ticks, parallel):
    waves = max(1, (max(1, fights) + max(1, parallel) - 1) // max(1, parallel))
    ideal_seconds = waves * max(1, duration_ticks) / 20.0
    return int(max(2400, ideal_seconds * 4.0 + 900))


def iter_jsonl(path):
    root = Path(path)
    files = [root] if root.is_file() else sorted(root.glob("*.jsonl"))
    for file in files:
        if file.suffix != ".jsonl":
            continue
        with file.open("r", encoding="utf-8") as handle:
            for line_no, line in enumerate(handle, 1):
                line = line.strip()
                if not line:
                    continue
                try:
                    obj = json.loads(line)
                except json.JSONDecodeError:
                    yield {"event": "parse_error", "file": str(file), "line": line_no}
                    continue
                obj["_file"] = str(file)
                obj["_line"] = line_no
                yield obj


def rate(num, den):
    return num / den if den else 0.0


def fmt(value):
    return f"{value:.3f}"


def analyze(export_dir, benchmark_id, win_gate):
    export_dir = Path(export_dir)
    preferred = sorted(export_dir.glob("op_sukuna_brain*.jsonl"))
    jsonl_source = preferred[0] if preferred else export_dir
    events = []
    parse_errors = 0
    for event in iter_jsonl(jsonl_source):
        if event.get("event") == "parse_error":
            parse_errors += 1
            continue
        if event.get("benchmark_id") == benchmark_id:
            events.append(event)
    if not events:
        for event in iter_jsonl(jsonl_source):
            if event.get("event") == "parse_error":
                parse_errors += 1
                continue
            events.append(event)

    decisions_by_run = defaultdict(list)
    samples_by_run = defaultdict(list)
    damage_by_run = defaultdict(list)
    contamination_by_run = defaultdict(list)
    lab_invalid_by_run = defaultdict(list)
    sukuna_damage_by_run = defaultdict(list)
    target_death_by_run = defaultdict(list)
    ai_gates_by_run = defaultdict(list)
    fight_ends = []
    action_counts = Counter()
    sampled_skill_counts = Counter()
    block_reasons = Counter()
    fallback_counts = Counter()
    target_types = Counter()
    gate_reasons = Counter()
    for event in events:
        name = event.get("event")
        run = int(event.get("run_index") or -1)
        if name == "decision" and run >= 0:
            decisions_by_run[run].append(event)
            action_counts[event.get("action", "unknown")] += 1
            target_types[event.get("target_type", "unknown")] += 1
            if not event.get("action_started", True):
                block_reasons[event.get("block_reason", "unknown")] += 1
            if event.get("fallback_action"):
                fallback_counts[event.get("fallback_action", "unknown")] += 1
        elif name == "benchmark_sample" and run >= 0:
            samples_by_run[run].append(event)
            sampled_skill_counts[str(int(float(event.get("sukuna_skill") or 0)))] += 1
            target_types[event.get("target_type", "unknown")] += 1
        elif name == "damage_resolved" and run >= 0:
            damage_by_run[run].append(event)
        elif name == "sukuna_damage_resolved" and run >= 0:
            sukuna_damage_by_run[run].append(event)
        elif name == "target_death_resolved" and run >= 0:
            target_death_by_run[run].append(event)
        elif name == "arena_contamination" and run >= 0:
            contamination_by_run[run].append(event)
        elif name == "lab_invalid" and run >= 0:
            lab_invalid_by_run[run].append(event)
        elif name == "ai_gate" and run >= 0:
            ai_gates_by_run[run].append(event)
            gate_reasons[f"{event.get('stage', 'unknown')}:{event.get('reason', 'unknown')}"] += 1
        elif name == "benchmark_fight_end":
            fight_ends.append(event)

    fights = []
    bad_windows = []
    outcomes = Counter()
    for end in sorted(fight_ends, key=lambda item: int(item.get("run_index") or 0)):
        run = int(end.get("run_index") or -1)
        decisions = decisions_by_run.get(run, [])
        samples = sorted(samples_by_run.get(run, []), key=lambda item: float(item.get("age") or item.get("game_time") or 0))
        synthesized = False
        if not decisions and samples:
            decisions = synthesize_decisions_from_samples(samples)
            synthesized = True
            for item in decisions:
                action_counts[item.get("action", "unknown")] += 1
                target_types[item.get("target_type", "unknown")] += 1
        damages = damage_by_run.get(run, [])
        contaminations = contamination_by_run.get(run, [])
        lab_invalid_events = lab_invalid_by_run.get(run, [])
        sukuna_damage_events = sukuna_damage_by_run.get(run, [])
        target_deaths = target_death_by_run.get(run, [])
        gates = ai_gates_by_run.get(run, [])
        gate_counter = Counter(f"{item.get('stage', 'unknown')}:{item.get('reason', 'unknown')}" for item in gates)
        main_target_uuid = samples[0].get("target_uuid") if samples else ""
        target_damages = [item for item in damages if not main_target_uuid or item.get("target_uuid") == main_target_uuid]
        splash_damages = [item for item in damages if main_target_uuid and item.get("target_uuid") != main_target_uuid]
        outcome = end.get("outcome", "unknown")
        outcomes[outcome] += 1
        blocked = sum(1 for item in decisions if not item.get("action_started", True))
        fallback = sum(1 for item in decisions if item.get("fallback_action"))
        no_impact = sum(1 for item in decisions if float(item.get("damage_dealt") or 0.0) <= 0.0 and float(item.get("damage_taken") or 0.0) <= 0.0)
        range_actions = sum(1 for item in decisions if item.get("kind") == "RANGE_CONTROL")
        decision_damage_dealt = sum(float(item.get("damage_dealt") or 0.0) for item in decisions)
        decision_damage_taken = sum(float(item.get("damage_taken") or 0.0) for item in decisions)
        resolved_damage_dealt = sum(float(item.get("actual_damage") or 0.0) for item in target_damages)
        resolved_splash_damage = sum(float(item.get("actual_damage") or 0.0) for item in splash_damages)
        resolved_damage_taken = sum(float(item.get("actual_damage") or 0.0) for item in sukuna_damage_events)
        first_real_damage_tick = first_damage_tick(target_damages)
        first_target_hurt_sample = first_hurt_sample(samples, "target_health")
        first_sukuna_hurt_sample = first_hurt_sample(samples, "sukuna_health")
        min_distance = min((float(item.get("distance") or 0.0) for item in samples), default=0.0)
        avg_distance = sum(float(item.get("distance") or 0.0) for item in samples) / len(samples) if samples else 0.0
        max_nearby_living = max((int(float(item.get("nearby_living_entities") or 0)) for item in samples), default=0)
        max_projectiles = max((int(float(item.get("nearby_projectiles") or 0)) for item in samples), default=0)
        sampled_skills = Counter(str(int(float(item.get("sukuna_skill") or 0))) for item in samples).most_common(8)
        first_offense = next((i for i, item in enumerate(decisions) if is_offensive_decision(item)), None)
        last = list(decisions[-12:])
        top = Counter(item.get("action", "unknown") for item in decisions).most_common(5)
        idle_chain_max = longest_idle_chain(decisions)
        high_cost_no_impact_count = count_high_cost_no_impact(decisions)
        details = parse_details(end.get("details", ""))
        first_effective_damage = (
            first_real_damage_tick if first_real_damage_tick >= 0
            else first_target_hurt_sample if first_target_hurt_sample >= 0
            else -1
        )
        fight = {
            "run_index": run,
            "arena_index": int(end.get("arena_index") or -1),
            "variant": end.get("variant", ""),
            "outcome": outcome,
            "decisions": len(decisions),
            "synthetic_decisions": synthesized,
            "samples": len(samples),
            "damage_events": len(damages),
            "sukuna_damage_events": len(sukuna_damage_events),
            "target_death_events": len(target_deaths),
            "arena_contamination_events": len(contaminations),
            "lab_invalid_events": len(lab_invalid_events),
            "lab_invalid_reason": lab_invalid_reason(end, lab_invalid_events),
            "ai_gate_events": len(gates),
            "gate_reasons": gate_counter.most_common(6),
            "target_damage_events": len(target_damages),
            "splash_damage_events": len(splash_damages),
            "blocked_actions": blocked,
            "fallbacks": fallback,
            "no_impact_decisions": no_impact,
            "range_actions": range_actions,
            "decision_damage_dealt": decision_damage_dealt,
            "decision_damage_taken": decision_damage_taken,
            "resolved_damage_dealt": resolved_damage_dealt,
            "resolved_splash_damage": resolved_splash_damage,
            "resolved_damage_taken": resolved_damage_taken,
            "first_real_damage_tick": first_real_damage_tick,
            "first_target_hurt_sample": first_target_hurt_sample,
            "first_sukuna_hurt_sample": first_sukuna_hurt_sample,
            "min_distance": min_distance,
            "avg_distance": avg_distance,
            "max_nearby_living": max_nearby_living,
            "max_projectiles": max_projectiles,
            "sampled_skills": sampled_skills,
            "first_offense_decision_index": -1 if first_offense is None else first_offense,
            "time_to_first_effective_damage": first_effective_damage,
            "idle_chain_max": idle_chain_max,
            "high_cost_no_impact_count": high_cost_no_impact_count,
            "top_actions": top,
            "ticks": int(float(details.get("ticks", 0) or 0)),
            "final_sukuna_health": float(details.get("sukuna_health", 0.0) or 0.0),
            "final_target_health": float(details.get("target_health", 0.0) or 0.0),
            "details": end.get("details", ""),
        }
        fight["failure_primary"] = primary_failure_tag(fight)
        fights.append(fight)
        suspicious = (
            outcome == "lab_invalid"
            or outcome != "target_dead"
            or ((not synthesized) and decisions and rate(blocked, len(decisions)) > 0.12)
            or ((not synthesized) and decisions and rate(range_actions, len(decisions)) > 0.45)
            or ((not synthesized) and decisions and rate(no_impact, len(decisions)) > 0.55)
            or first_real_damage_tick < 0
            or first_real_damage_tick > 160
            or resolved_damage_dealt <= 0.0
        )
        if suspicious:
            bad_windows.append({
                "run_index": run,
                "outcome": outcome,
                "variant": end.get("variant", ""),
                "symptoms": symptoms_for(fight),
                "samples_summary": {
                    "samples": len(samples),
                    "first_real_damage_tick": first_real_damage_tick,
                    "resolved_damage_dealt": resolved_damage_dealt,
                    "resolved_splash_damage": resolved_splash_damage,
                    "avg_distance": avg_distance,
                    "min_distance": min_distance,
                    "sampled_skills": sampled_skills,
                    "max_nearby_living": max_nearby_living,
                    "max_projectiles": max_projectiles,
                    "arena_contamination_events": len(contaminations),
                    "lab_invalid_events": len(lab_invalid_events),
                    "lab_invalid_reason": lab_invalid_reason(end, lab_invalid_events),
                    "ai_gate_events": len(gates),
                    "gate_reasons": gate_counter.most_common(6),
                },
                "last_samples": compact_samples(samples[-8:]),
                "last_decisions": [
                    {
                        "action": item.get("action"),
                        "kind": item.get("kind"),
                        "started": item.get("action_started", True),
                        "block_reason": item.get("block_reason", ""),
                        "fallback": item.get("fallback_action", ""),
                        "damage_dealt": item.get("damage_dealt", 0),
                        "damage_taken": item.get("damage_taken", 0),
                        "distance": item.get("distance", 0),
                        "path_blocked": item.get("path_blocked", False),
                        "no_impact_streak": item.get("no_impact_action_streak", 0),
                        "range_streak": item.get("range_action_streak", 0),
                    }
                    for item in last
                ],
            })

    wins = outcomes["target_dead"]
    total = len(fight_ends)
    invalid_lab_fights = sum(1 for fight in fights if fight["outcome"] == "lab_invalid" or fight.get("lab_invalid_events", 0) > 0)
    valid_fights = max(0, total - invalid_lab_fights)
    win_rate = rate(wins, total)
    win_rate_valid = rate(wins, valid_fights)
    real_decision_fights = sum(1 for fight in fights if not fight["synthetic_decisions"] and fight["decisions"] > 0)
    synthetic_decision_fights = sum(1 for fight in fights if fight["synthetic_decisions"])
    missing_decision_fights = sum(1 for fight in fights if fight["decisions"] <= 0 and not fight["synthetic_decisions"])
    contamination_fights = sum(1 for fight in fights if fight.get("arena_contamination_events", 0) > 0)
    lab_invalid_reasons = Counter(fight.get("lab_invalid_reason") or "unknown_lab_failure" for fight in fights if fight["outcome"] == "lab_invalid" or fight.get("lab_invalid_events", 0) > 0)
    telemetry_valid = invalid_lab_fights == 0 and contamination_fights == 0 and missing_decision_fights == 0 and parse_errors == 0 and real_decision_fights > 0
    telemetry_invalid_reason = ""
    if not telemetry_valid:
        telemetry_invalid_reason = (
            f"invalid_lab_fights={invalid_lab_fights},"
            f"contamination_fights={contamination_fights},"
            f"missing_decision_fights={missing_decision_fights},"
            f"parse_errors={parse_errors},"
            f"real_decision_fights={real_decision_fights}"
        )
    summary = {
        "benchmark_id": benchmark_id,
        "total_fights": total,
        "valid_fights": valid_fights,
        "invalid_fights": invalid_lab_fights,
        "wins": wins,
        "win_rate": win_rate,
        "win_rate_valid": win_rate_valid,
        "passed": invalid_lab_fights == 0 and win_rate_valid >= win_gate,
        "win_gate": win_gate,
        "outcomes": dict(outcomes),
        "lab_invalid_reasons": lab_invalid_reasons.most_common(12),
        "top_actions": action_counts.most_common(12),
        "sampled_skills": sampled_skill_counts.most_common(12),
        "block_reasons": block_reasons.most_common(12),
        "fallbacks": fallback_counts.most_common(12),
        "target_types": target_types.most_common(8),
        "gate_reasons": gate_reasons.most_common(12),
        "bad_windows": len(bad_windows),
        "arena_contamination_fights": contamination_fights,
        "damage": damage_summary(fights),
        "variants": variant_summary(fights),
        "parse_errors": parse_errors,
        "telemetry_quality": {
            "valid": telemetry_valid,
            "real_decision_fights": real_decision_fights,
            "synthetic_decision_fights": synthetic_decision_fights,
            "missing_decision_fights": missing_decision_fights,
            "contamination_fights": contamination_fights,
            "invalid_lab_fights": invalid_lab_fights,
            "invalid_reason": telemetry_invalid_reason,
        },
        "generated_by": "python_postprocess",
    }
    write_outputs(export_dir, summary, fights, bad_windows)
    return summary


def lab_invalid_reason(end_event, lab_events):
    details = parse_details(end_event.get("details", ""))
    if details.get("lab_reason"):
        return details["lab_reason"]
    for event in lab_events:
        reason = str(event.get("outcome") or "").strip()
        if reason:
            return reason
        event_details = parse_details(event.get("details", ""))
        if event_details.get("reason"):
            return event_details["reason"]
    return ""


def symptoms_for(fight):
    symptoms = []
    decisions = max(1, fight["decisions"])
    if fight.get("lab_invalid_events", 0) > 0 or fight.get("outcome") == "lab_invalid":
        symptoms.append(f"lab_invalid:{fight.get('lab_invalid_reason') or 'unknown_lab_failure'}")
    if fight["outcome"] != "target_dead":
        symptoms.append(f"outcome={fight['outcome']}")
    if fight["decisions"] <= 0:
        symptoms.append("missing_decision_telemetry")
    if fight.get("synthetic_decisions"):
        symptoms.append("telemetry_incomplete")
        gate_list = fight.get("gate_reasons") or []
        if gate_list:
            symptoms.append(f"ai_gate:{gate_list[0][0]}")
    if fight["samples"] <= 0:
        symptoms.append("missing_lab_samples")
    if fight["first_real_damage_tick"] < 0:
        symptoms.append("no_true_damage")
    elif fight["first_real_damage_tick"] > 160:
        symptoms.append("slow_first_true_damage")
    if fight["resolved_damage_dealt"] <= 0.0:
        symptoms.append("zero_resolved_damage")
    if fight.get("arena_contamination_events", 0) > 0:
        symptoms.append("arena_contamination")
    if (not fight.get("synthetic_decisions")) and rate(fight["blocked_actions"], decisions) > 0.12:
        symptoms.append("blocked_actions_high")
    if (not fight.get("synthetic_decisions")) and rate(fight["fallbacks"], decisions) > 0.10:
        symptoms.append("fallback_high")
    if (not fight.get("synthetic_decisions")) and rate(fight["range_actions"], decisions) > 0.45:
        symptoms.append("timid_range_loop")
    if (not fight.get("synthetic_decisions")) and rate(fight["no_impact_decisions"], decisions) > 0.55:
        symptoms.append("no_impact_loop")
    if fight["first_offense_decision_index"] > 12 or fight["first_offense_decision_index"] < 0:
        symptoms.append("slow_first_offense")
    return symptoms


def parse_details(details):
    result = {}
    for item in str(details or "").split(","):
        if "=" not in item:
            continue
        key, value = item.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def first_damage_tick(damages):
    ticks = [
        int(float(item.get("sukuna_tick") or item.get("game_time") or 0))
        for item in damages
        if float(item.get("actual_damage") or 0.0) > 0.0
    ]
    return min(ticks) if ticks else -1


def first_hurt_sample(samples, health_key):
    if not samples:
        return -1
    start = float(samples[0].get(health_key) or 0.0)
    for item in samples[1:]:
        if float(item.get(health_key) or 0.0) < start:
            return int(float(item.get("age") or 0))
    return -1


def compact_samples(samples):
    compact = []
    for item in samples:
        compact.append({
            "age": item.get("age", 0),
            "sukuna_health": item.get("sukuna_health", 0),
            "target_health": item.get("target_health", 0),
            "distance": item.get("distance", 0),
            "sukuna_skill": item.get("sukuna_skill", 0),
            "target_skill": item.get("target_skill", 0),
            "sukuna_effects": item.get("sukuna_effects", ""),
            "target_effects": item.get("target_effects", ""),
            "nearby_living": item.get("nearby_living_entities", 0),
            "nearby_projectiles": item.get("nearby_projectiles", 0),
        })
    return compact


def is_offensive_decision(item):
    kind = str(item.get("kind") or "")
    if kind in ("MELEE", "NORMAL_SLASH", "WORLD_CUT", "DOMAIN", "TEN_SHADOWS", "TRANSFORM"):
        return True
    action = str(item.get("action") or "")
    if action == "OBSERVED_NO_TARGET_LOCK" or action == "OBSERVED_IDLE":
        return False
    return action.startswith("OBSERVED_SKILL_")


def longest_idle_chain(decisions):
    best = 0
    cur = 0
    for item in decisions:
        action = str(item.get("action") or "")
        if action in ("OBSERVED_IDLE", "OBSERVED_NO_TARGET_LOCK"):
            cur += 1
            best = max(best, cur)
        else:
            cur = 0
    return best


def count_high_cost_no_impact(decisions):
    count = 0
    for item in decisions:
        action = str(item.get("action") or "")
        if action in ("OBSERVED_SKILL_4204", "OBSERVED_SKILL_120", "OBSERVED_SKILL_4202"):
            if float(item.get("damage_dealt") or 0.0) <= 0.0:
                count += 1
    return count


def primary_failure_tag(fight):
    if fight["outcome"] == "lab_invalid":
        return "lab_invalid"
    if fight["outcome"] == "timeout":
        return "timeout"
    if fight["outcome"] != "target_dead" and fight["resolved_damage_dealt"] <= 0.0:
        return "no_true_damage"
    if fight["outcome"] != "target_dead" and fight["first_offense_decision_index"] < 0:
        return "slow_open"
    decisions = max(1, fight["decisions"])
    if rate(fight["no_impact_decisions"], decisions) > 0.55:
        return "no_impact_loop"
    if fight["outcome"] != "target_dead" and fight["decision_damage_taken"] > fight["decision_damage_dealt"] * 1.2:
        return "lost_trade"
    if fight["outcome"] != "target_dead":
        return "loss_misc"
    if fight["first_offense_decision_index"] < 0:
        return "slow_open_win"
    return "win_clean"


def synthesize_decisions_from_samples(samples):
    decisions = []
    previous = None
    for item in samples:
        skill = int(float(item.get("sukuna_skill") or 0))
        target_health = float(item.get("target_health") or 0.0)
        sukuna_health = float(item.get("sukuna_health") or 0.0)
        prev_target = target_health if previous is None else float(previous.get("target_health") or target_health)
        prev_sukuna = sukuna_health if previous is None else float(previous.get("sukuna_health") or sukuna_health)
        action = f"OBSERVED_SKILL_{skill}" if skill != 0 else "OBSERVED_IDLE"
        if skill == 0 and float(item.get("sukuna_cnt_target") or 0.0) <= 6.0:
            action = "OBSERVED_NO_TARGET_LOCK"
        decisions.append({
            "event": "decision",
            "run_index": item.get("run_index", -1),
            "arena_index": item.get("arena_index", -1),
            "variant": item.get("variant", ""),
            "action": action,
            "kind": "OBSERVED_EXECUTION" if skill != 0 else "OBSERVED_IDLE",
            "action_started": True,
            "block_reason": "synthesized_from_benchmark_sample",
            "fallback_action": "",
            "damage_dealt": max(0.0, prev_target - target_health),
            "damage_taken": max(0.0, prev_sukuna - sukuna_health),
            "distance": item.get("distance", 0),
            "path_blocked": False,
            "no_impact_action_streak": 0,
            "range_action_streak": 0,
            "target_type": item.get("target_type", "unknown"),
            "sukuna_health": sukuna_health,
            "target_health": target_health,
            "target_skill": item.get("target_skill", 0),
            "sukuna_effects": item.get("sukuna_effects", ""),
            "target_effects": item.get("target_effects", ""),
        })
        previous = item
    return decisions


def damage_summary(fights):
    total = len(fights)
    return {
        "avg_resolved_damage_dealt": sum(item["resolved_damage_dealt"] for item in fights) / total if total else 0.0,
        "avg_splash_damage": sum(item["resolved_splash_damage"] for item in fights) / total if total else 0.0,
        "no_true_damage_fights": sum(1 for item in fights if item["resolved_damage_dealt"] <= 0.0),
        "slow_first_damage_fights": sum(1 for item in fights if item["first_real_damage_tick"] < 0 or item["first_real_damage_tick"] > 160),
    }


def variant_summary(fights):
    by_variant = defaultdict(list)
    for fight in fights:
        by_variant[fight["variant"]].append(fight)
    result = {}
    for variant, items in by_variant.items():
        result[variant] = {
            "fights": len(items),
            "wins": sum(1 for item in items if item["outcome"] == "target_dead"),
            "win_rate": rate(sum(1 for item in items if item["outcome"] == "target_dead"), len(items)),
            "avg_damage_dealt": sum(item["resolved_damage_dealt"] for item in items) / len(items),
            "avg_first_damage_tick": sum(max(item["first_real_damage_tick"], 9999) for item in items) / len(items),
        }
    return result


def write_outputs(export_dir, summary, fights, bad_windows):
    export_dir.mkdir(parents=True, exist_ok=True)
    atomic_write_text(export_dir / "benchmark_summary.json", json.dumps(summary, indent=2, ensure_ascii=False) + "\n")
    with atomic_writer(export_dir / "fight_summaries.jsonl") as handle:
        for fight in fights:
            handle.write(json.dumps(fight, ensure_ascii=False) + "\n")
    with atomic_writer(export_dir / "bad_windows.jsonl") as handle:
        for window in bad_windows:
            handle.write(json.dumps(window, ensure_ascii=False) + "\n")
    lines = [
        "# OP Sukuna Benchmark",
        "",
        f"- quality: `{'INVALID_LAB' if summary.get('invalid_fights', 0) else ('VALID_FOR_AI_TUNING' if summary.get('telemetry_quality', {}).get('valid') else 'INVALID_FOR_AI_TUNING')}`",
        f"- benchmark_id: `{summary['benchmark_id']}`",
        f"- fights: {summary['total_fights']}",
        f"- valid_fights: {summary.get('valid_fights', summary['total_fights'])}",
        f"- invalid_fights: {summary.get('invalid_fights', 0)}",
        f"- wins: {summary['wins']}",
        f"- win_rate: {fmt(summary['win_rate'])}",
        f"- win_rate_valid: {fmt(summary.get('win_rate_valid', summary['win_rate']))}",
        f"- gate: {fmt(summary['win_gate'])}",
        f"- passed: {summary['passed']}",
        f"- outcomes: `{summary['outcomes']}`",
        f"- lab_invalid_reasons: `{summary.get('lab_invalid_reasons', [])}`",
        f"- gate_reasons: `{summary.get('gate_reasons', [])}`",
        f"- bad_windows: {summary['bad_windows']}",
        f"- arena_contamination_fights: {summary['arena_contamination_fights']}",
        f"- telemetry_quality: `{summary.get('telemetry_quality', {})}`",
        "",
        "## Top Actions",
    ]
    lines += [f"- {name}: {count}" for name, count in summary["top_actions"]]
    lines += ["", "## Sampled Skills"]
    lines += [f"- skill {name}: {count}" for name, count in summary["sampled_skills"]] or ["- none"]
    lines += ["", "## Damage"]
    lines += [f"- {name}: {value}" for name, value in summary["damage"].items()]
    lines += ["", "## Variants"]
    lines += [f"- {name}: {value}" for name, value in summary["variants"].items()]
    lines += ["", "## Block Reasons"]
    lines += [f"- {name}: {count}" for name, count in summary["block_reasons"]] or ["- none"]
    lines += ["", "## Fallbacks"]
    lines += [f"- {name}: {count}" for name, count in summary["fallbacks"]] or ["- none"]
    lines += ["", "## AI Gate Reasons"]
    lines += [f"- {name}: {count}" for name, count in summary.get("gate_reasons", [])] or ["- none"]
    lines += ["", "## Files", "- `benchmark_summary.json`", "- `fight_summaries.jsonl`", "- `bad_windows.jsonl`"]
    atomic_write_text(export_dir / "benchmark_report.md", "\n".join(lines) + "\n")


def atomic_write_text(path, content):
    path = Path(path)
    with NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as tmp:
        tmp.write(content)
        tmp.flush()
        os.fsync(tmp.fileno())
        tmp_name = tmp.name
    os.replace(tmp_name, path)


class atomic_writer:
    def __init__(self, path):
        self.path = Path(path)
        self.tmp = None
        self.handle = None

    def __enter__(self):
        self.tmp = NamedTemporaryFile("w", encoding="utf-8", dir=self.path.parent, delete=False)
        self.handle = self.tmp
        return self.handle

    def __exit__(self, exc_type, exc, tb):
        try:
            if self.handle:
                self.handle.flush()
                os.fsync(self.handle.fileno())
                self.handle.close()
        finally:
            if exc_type is None:
                os.replace(self.tmp.name, self.path)
            else:
                try:
                    os.unlink(self.tmp.name)
                except OSError:
                    pass


def send_command(process, command):
    if process.stdin is None:
        raise RuntimeError("runServer stdin is unavailable")
    process.stdin.write(command + "\n")
    process.stdin.flush()


def stop_server_and_wait(process, timeout=60):
    if process.poll() is not None:
        return
    try:
        print("[bench] benchmark finished; sending server stop", flush=True)
        send_command(process, "stop")
    except Exception:
        try:
            process.send_signal(signal.SIGTERM)
        except Exception:
            pass

    # Drain stdout in a daemon thread so the pipe buffer never blocks the
    # server from shutting down, without blocking the main thread.
    import threading
    def _drain():
        try:
            for _ in process.stdout:
                pass
        except Exception:
            pass
    drain_thread = threading.Thread(target=_drain, daemon=True)
    drain_thread.start()

    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        print("[bench] stop timeout; killing server process", flush=True)
        process.kill()
        try:
            process.wait(timeout=10)
        except Exception:
            pass

    drain_thread.join(timeout=5)


def recover_latest_export(repo_root, min_mtime=0):
    bench_root = Path(repo_root) / "run" / "jjkur-op-ai-metrics" / "benchmarks"
    if not bench_root.exists():
        return None
    dirs = sorted([
        path for path in bench_root.iterdir()
        if path.is_dir() and path.stat().st_mtime >= max(0, min_mtime - 5)
    ], key=lambda p: p.stat().st_mtime)
    return dirs[-1] if dirs else None


def recover_benchmark_id(export_path):
    summary_path = Path(export_path) / "benchmark_summary.json"
    if summary_path.exists():
        try:
            obj = json.loads(summary_path.read_text(encoding="utf-8"))
            value = str(obj.get("benchmark_id") or "").strip()
            if value:
                return value
        except Exception:
            pass
    return Path(export_path).name.strip()


def summary_exit_code(summary):
    if summary.get("invalid_fights", 0) or not summary.get("telemetry_quality", {}).get("valid", False):
        return 2
    return 0 if summary.get("passed") else 1


def main():
    args = parse_args()
    repo = Path(args.repo).resolve()
    telemetry_profile = args.telemetry_profile
    sample_interval = args.sample_interval if args.sample_interval > 0 else sample_interval_for_profile(telemetry_profile)

    if args.analyze_only:
        export_dir = Path(args.analyze_only).resolve()
        if not export_dir.exists():
            print(f"[bench] analyze-only path not found: {export_dir}", file=sys.stderr)
            return 2
        benchmark_id = args.benchmark_id.strip() or recover_benchmark_id(export_dir)
        summary = analyze(export_dir, benchmark_id, 0.70)
        print(f"[bench] analyze-only report: {export_dir / 'benchmark_report.md'}", flush=True)
        print(f"[bench] analyze-only win_rate={fmt(summary['win_rate'])} passed={summary['passed']}", flush=True)
        return summary_exit_code(summary)

    def run_once(label, fights, duration, parallel, stop_server):
        env = os.environ.copy()
        env["JJKU_OP_SUKUNA_METRICS_DEV"] = "true"
        env["JJKU_OP_SUKUNA_BENCH"] = "true"
        env["JJKU_OP_SUKUNA_BENCH_SCENARIO"] = args.scenario
        env["JJKU_OP_SUKUNA_BENCH_FIGHTS"] = str(fights)
        env["JJKU_OP_SUKUNA_BENCH_DURATION"] = str(duration)
        env["JJKU_OP_SUKUNA_BENCH_PARALLEL"] = str(parallel)
        env["JJKU_OP_SUKUNA_BENCH_VARIANT_SET"] = args.variant_set
        env["JJKU_OP_SUKUNA_BENCH_AUTO_STOP"] = "true" if stop_server else "false"
        env["JJKU_OP_SUKUNA_BENCH_ARENA_SPACING"] = str(max(64, min(args.arena_spacing, 4096)))
        env["JJKU_OP_SUKUNA_BENCH_ARENA_STARTS_PER_TICK"] = str(max(1, min(args.arena_starts_per_tick, 8)))
        env["JJKU_OP_SUKUNA_BENCH_TELEMETRY_PROFILE"] = telemetry_profile
        env["JJKU_OP_SUKUNA_BENCH_SAMPLE_INTERVAL"] = str(max(2, min(sample_interval, 80)))
        cmd = ["./gradlew", "--no-daemon", "--console=plain", "runServer"]
        print(f"[bench] [{label}] starting: {' '.join(cmd)}", flush=True)
        process = subprocess.Popen(
            cmd,
            cwd=repo,
            env=env,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        started_at = time.time()
        ready_deadline = started_at + args.ready_timeout
        bench_deadline = None
        started = False
        ready = False
        benchmark_id = ""
        export_dir = None
        benchmark_timeout = args.benchmark_timeout if args.benchmark_timeout > 0 else auto_benchmark_timeout(fights, duration, parallel)
        timeout_stop_requested = False
        marker = re.compile(r"JJKU_BENCH_FINISHED (?P<summary>.*?export=(?P<export>\\S+))")
        id_marker = re.compile(r"benchmark_id=(?P<id>[^,\\s]+)")
        try:
            assert process.stdout is not None
            for line in process.stdout:
                print(line, end="")
                now = time.time()
                if process.poll() is not None:
                    break
                if "JJKU_BENCH_STARTED" in line:
                    started = True
                    bench_deadline = now + benchmark_timeout
                    print(f"[bench] [{label}] benchmark timeout: {benchmark_timeout}s", flush=True)
                if not ready and any(pattern in line for pattern in READY_PATTERNS):
                    ready = True
                if ready and not started:
                    command = f"jjkuopai bench run {args.scenario} {fights} {duration} {parallel} {args.variant_set}"
                    try:
                        print(f"[bench] [{label}] auto-start marker not seen; sending fallback command: {command}", flush=True)
                        send_command(process, command)
                    except Exception as exc:
                        print(f"[bench] [{label}] fallback command failed: {exc}", flush=True)
                    started = True
                    bench_deadline = now + benchmark_timeout
                    print(f"[bench] [{label}] benchmark timeout: {benchmark_timeout}s", flush=True)
                if not ready and now > ready_deadline:
                    raise TimeoutError(f"[{label}] server did not become ready before --ready-timeout")
                found = marker.search(line)
                if found:
                    export_dir = Path(found.group("export"))
                    id_found = id_marker.search(found.group("summary"))
                    benchmark_id = id_found.group("id") if id_found else ""
                    break
                if bench_deadline is not None and now > bench_deadline:
                    if not timeout_stop_requested:
                        timeout_stop_requested = True
                        bench_deadline = now + max(30, args.benchmark_stop_timeout)
                        try:
                            print(f"[bench] [{label}] benchmark timeout reached; requesting partial export with: jjkuopai bench stop", flush=True)
                            send_command(process, "jjkuopai bench stop")
                        except Exception as exc:
                            print(f"[bench] [{label}] failed to request benchmark stop: {exc}", flush=True)
                        continue
                    raise TimeoutError(f"[{label}] benchmark did not finish after forced partial export timeout")
        finally:
            if process.poll() is None and stop_server:
                stop_server_and_wait(process)
        if process.returncode not in (0, None):
            print(f"[bench] [{label}] server process exited with code {process.returncode}", file=sys.stderr)
        if not export_dir:
            export_dir = recover_latest_export(repo, started_at)
            if export_dir:
                print(f"[bench] [{label}] finish marker missing; recovered latest export dir: {export_dir}", flush=True)
        if export_dir and not benchmark_id:
            benchmark_id = recover_benchmark_id(export_dir)
        if not export_dir or not benchmark_id:
            raise RuntimeError(f"[{label}] benchmark finish marker was not found")
        summary = analyze(export_dir, benchmark_id, 0.70)
        elapsed = time.time() - started_at
        print(f"[bench] [{label}] report: {export_dir / 'benchmark_report.md'}", flush=True)
        print(f"[bench] [{label}] win_rate={fmt(summary['win_rate'])} win_rate_valid={fmt(summary.get('win_rate_valid', summary['win_rate']))} passed={summary['passed']} invalid_fights={summary.get('invalid_fights', 0)} elapsed_sec={elapsed:.1f}", flush=True)
        return {"summary": summary, "export_dir": export_dir, "benchmark_id": benchmark_id, "elapsed": elapsed}

    parallel = effective_parallel(args.parallel, args.max_parallel, args.auto_parallel, telemetry_profile, args.fights)

    if args.parallel_probe and args.parallel in ("auto", "max"):
        probe_values = []
        for token in args.parallel_probe_values.split(","):
            token = token.strip()
            if not token:
                continue
            try:
                value = int(token)
            except ValueError:
                continue
            if value > 0:
                probe_values.append(value)
        if probe_values:
            cap = max(1, min(args.max_parallel, 32))
            probe_values = sorted({max(1, min(cap, x)) for x in probe_values})
            best = None
            for value in probe_values:
                result = run_once(f"probe-p{value}", args.parallel_probe_fights, args.parallel_probe_duration, value, True)
                score = (result["summary"]["win_rate"], -result["elapsed"])
                if best is None or score > best["score"]:
                    best = {"score": score, "parallel": value}
            if best is not None:
                parallel = best["parallel"]
                print(f"[bench] selected parallel from probe: {parallel}", flush=True)

    try:
        if args.two_phase:
            phase1_parallel = effective_parallel(args.parallel, args.max_parallel, args.auto_parallel, telemetry_profile, args.phase1_fights)
            phase1 = run_once("phase1", args.phase1_fights, args.phase1_duration, phase1_parallel, args.stop_server)
            phase1_exit = summary_exit_code(phase1["summary"])
            if phase1_exit == 2:
                return 2
            if phase1["summary"]["win_rate"] < args.phase1_gate:
                print(f"[bench] phase1 gate failed: win_rate={fmt(phase1['summary']['win_rate'])} < gate={fmt(args.phase1_gate)}; skipping phase2", flush=True)
                return 1
            phase2 = run_once("phase2", args.fights, args.duration, parallel, args.stop_server)
            return summary_exit_code(phase2["summary"])
        single = run_once("single", args.fights, args.duration, parallel, args.stop_server)
        return summary_exit_code(single["summary"])
    except TimeoutError as exc:
        print(f"[bench] timeout: {exc}", file=sys.stderr)
        return 2
    except RuntimeError as exc:
        print(f"[bench] error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
