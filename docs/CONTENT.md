# Biohacker — the catalogue of protocols

Biohacker («Биохакер») is a library of protocols, habits and techniques for a longer, healthier, calmer and
happier life, played as a lab. The body is drawn as a blueprint with eight **systems**. Every protocol is a
**mod**. The player puts a mod on **trial** for 14 days (an n=1 experiment with five daily gauges: sleep,
energy, mood, focus, calm), then installs it into their "firmware" if it worked for them, or rejects it.
A card a day arrives as a sealed parcel, with a rarity; debunked hacks are collected as "broken mods".

This file is the brief for writing the catalogue.

## Files and format

One TSV per system in `app/src/main/assets/content/`, UTF-8, no header, tab-separated, one mod per line,
**17 columns**:

```
id	sys	tag	ev	rare	load	when	min	freq	ru_name	en_name	ru_how	en_how	ru_why	en_why	ru_warn	en_warn
sleep-001	sleep	light	A	1	1	wake	15	7	Утренний свет	Morning light	В первый час после подъёма выйди на улицу на 10–15 минут без солнечных очков. В пасмурный день — дольше	Within an hour of waking, go outside for 10–15 minutes without sunglasses. Longer on a cloudy day	Яркий утренний свет — главный сигнал для внутренних часов: вечером раньше приходит сонливость, утром легче проснуться	Bright morning light is the strongest cue for the body clock: you get sleepy earlier at night and wake more easily	Не смотри прямо на солнце	Never look straight at the sun
```

- `id` — `<sys>-<NNN>`, three digits from 001, unique, never reused.
- `sys` — the system id (the file name).
- `tag` — one of the six tags of that system (table below). Tags drive the hidden **combos**, so pick honestly.
- `ev` — evidence class, be strict and honest:
  - `A` — strong: several RCTs / meta-analyses, broad scientific consensus (e.g. regular aerobic exercise, sleep regularity, not smoking).
  - `B` — good: some RCTs or large, consistent cohort studies.
  - `C` — plausible: small studies, mechanism, observational links, mixed results.
  - `D` — weak: tradition, expert opinion, anecdote — but harmless, cheap and pleasant enough to try.
- `rare` — how surprising the card is when it drops: `1` common (a well-known basic), `2` rare (less known, specific),
  `3` legendary (surprising, little-known, yet legit). Aim for roughly 60 % / 30 % / 10 %.
- `load` — how much of a person's willpower it takes while it is still new: `1` light (a tiny tweak), `2` medium,
  `3` heavy (a real change of routine). Aim for roughly 45 % / 40 % / 15 %.
- `when` — time of day: `wake` (right after waking), `morning`, `day`, `evening`, `bed` (the last hour before
  sleep), `any`.
- `min` — minutes it takes per time at most: `1`, `5`, `15`, `30` or `60` (60 = an hour or more). For "don't do X"
  mods use the effort to keep it, usually `1`.
- `freq` — how many days a week it is done: `1`..`7` (7 = daily, 3 = three times a week, 1 = weekly).
- `ru_name`, `en_name` — the mod's name, 2–5 words, ≤ 34 characters, no final full stop. A name like a
  module: «Утренний свет», «Холодный финал душа», «Ужин за 3 часа до сна».
- `ru_how`, `en_how` — exactly what to do, concrete, with doses and times, imperative, ≤ 220 characters, no final
  full stop. One or two sentences.
- `ru_why`, `en_why` — why it works, in plain words, ≤ 220 characters, no final full stop. Mechanism or what
  studies found. **Never invent numbers, study names, percentages or quotes.** Prefer qualitative wording
  («снижает», «связано с») unless a figure is textbook-solid (e.g. caffeine half-life ~5–6 h).
- `ru_warn`, `en_warn` — a short caution or contraindication, ≤ 140 characters, no final full stop, or `-` when
  there is none. Required for anything with cold, heat, fasting, intense exercise, breath holds, supplements,
  sun, and anything where a medical condition matters.

Russian uses «ёлочки», em dashes — and «ё», addresses the player as «ты». English is natural, not a
word-for-word translation. Units: metric (°C, km, g, ml).

## How many

**At least 120 mods per system, aim for 140.** Inside a system:

- every tag has at least 14 mods;
- at least 40 mods take ≤ 5 minutes;
- at least 30 % have `ev` A or B, and at most 25 % are `D`;
- every `when` that makes sense for the system is used; `any` at most for half;
- `load` 1 for at least a third.

## Rules — every mod must be

- **Safe.** Nothing that risks health for an ordinary adult. No prescription drugs, hormones, peptides,
  "nootropic" drugs, megadoses, extreme fasting (eating windows no shorter than 8 hours; no multi-day fasts),
  ice baths without a warning, breath holds under water, hyperventilation while driving or in water, sun
  without protection, sleep deprivation, unregulated devices. Supplements: only very well-studied ones at
  ordinary doses (vitamin D when low, creatine, omega-3 via fish, caffeine timing, magnesium from food first),
  at most 8 per file, always with a «посоветуйся с врачом / check with your doctor» warning.
- **Honest.** The evidence class matches reality. Don't promise longevity from a single trick. Popular but
  unsupported hacks belong in the myths file, not here.
- **Concrete.** A clear thing to do: «Ложись и вставай в одно время ±30 минут, даже в выходные», not
  «Высыпайся».
- **Ordinary.** Possible in normal city life, cheap or free, no special equipment beyond common things (a
  timer, a notebook, a pair of trainers). Gadgets only when cheap and optional (a sleep mask, earplugs).
- **Varied.** No near-duplicates inside a file. Vary across alone / with others / at home / at work / outside,
  quick tweaks and real routines, beginner and advanced.
- **Kind to the reader.** No shaming about weight, food or bodies; no moralising. Food mods talk about adding
  good things more than banning.

## Systems and tags

| sys | system (ru / en) | covers | tags (key — meaning) |
|---|---|---|---|
| `sleep` | Часы / Clock | sleep, circadian rhythm, light and darkness | `light` morning/daylight exposure · `dark` evening darkness, screens, blue light · `rhythm` regular schedule, timing, caffeine/alcohol timing for sleep · `room` bedroom: cool, dark, quiet, bed only for sleep · `winddown` pre-sleep rituals, wind-down, what to do when you can't sleep · `nap` naps, rest breaks, recovery after bad nights |
| `move` | Мотор / Engine | physical activity, fitness, mobility | `walk` walking, steps, stairs · `cardio` zone 2, running, cycling, swimming, intervals · `strength` resistance training, bodyweight, grip · `mobility` stretching, joints, yoga-like flows · `neat` breaking up sitting, moving through the day, posture · `balance` balance, coordination, agility, falls prevention |
| `fuel` | Топка / Furnace | nutrition, metabolism, hydration | `plants` vegetables, fruit, legumes, whole grains, nuts, fibre, variety · `protein` protein amount and spread, fish, eggs, dairy · `timing` meal timing, eating window, early dinner, breakfast · `water` hydration, drinks, coffee and tea · `less` less sugar, ultra-processed food, alcohol, salt — gentle swaps · `gut` fermented food, eating slowly, chewing, satiety, mindful eating |
| `shield` | Броня / Armor | hormesis, prevention, body care, environment | `cold` cold exposure · `heat` sauna, hot baths, heat · `sun` sunlight, vitamin D, skin care and protection · `teeth` teeth, gums, mouth, eyes, ears, posture care · `check` prevention, self-measurement (blood pressure, waist), check-ups, vaccines, safety habits · `air` home environment: air, ventilation, noise, chemicals, plants, smoke-free |
| `mind` | Процессор / Processor | focus, cognition, learning, digital hygiene | `focus` deep work, single-tasking, attention, Pomodoro · `learn` learning techniques, memory, languages, spaced repetition · `digital` phone and screen hygiene, notifications, social media · `create` making things, writing, drawing, music-making, problem solving · `read` reading, books, long-form · `novelty` new experiences, puzzles, games, new routes, curiosity |
| `calm` | Охлаждение / Cooling | stress, nervous system, emotions regulation | `breath` breathing techniques (slow breathing, physiological sigh, box breathing) · `meditate` meditation, mindfulness, attention training · `body` body scan, progressive muscle relaxation, yoga nidra, shaking out tension · `write` expressive writing, worry time, planning to unload the head · `slow` pauses, slowing down, boundaries with time, saying no, rest without guilt · `sense` grounding through the senses: sound, smell, touch, warmth, water |
| `joy` | Аккумулятор / Battery | mood, happiness, meaning | `grateful` gratitude practices · `play` play, fun, hobbies, laughter · `meaning` values, purpose, goals, legacy, future self · `savor` savouring, slowing down pleasures, anticipation, nostalgia · `music` listening, singing, dancing · `awe` awe, beauty, art, sky, big views |
| `link` | Сеть / Network | relationships, community, nature | `friends` friendships, keeping in touch, deep talks · `family` partner, kids, parents, relatives · `kind` kindness, helping, generosity, volunteering · `nature` time in nature, green and blue spaces, gardening, animals · `community` neighbours, clubs, groups, belonging · `touch` hugs, touch, presence, eye contact, listening |

## Myths — broken mods

One file per system, `app/src/main/assets/content/myths-<sys>.tsv`, **6 columns**, at least **10 per system**:

```
id	sys	ru_name	en_name	ru_truth	en_truth
sleep-m01	sleep	Добрать сон в выходные	Catching up on weekend sleep	Отсыпание в выходные лишь частично возвращает долг и сдвигает часы — в понедельник засыпать труднее	Weekend lie-ins only partly repay sleep debt and shift your clock, so Monday night gets harder
```

- `id` — `<sys>-mNN`.
- `ru_name`, `en_name` — the hack as people say it, ≤ 34 characters.
- `ru_truth`, `en_truth` — what the evidence actually says, ≤ 220 characters, no final full stop, fair and
  non-mocking (some myths are half-true: say which half).

## Check

`python tools/check_content.py` validates the files: columns, ids, enums, lengths, counts per tag and the
mix rules, and duplicate names/texts. Run it until it prints no errors; fix warnings where reasonable.
