import json

# Read the existing tasks
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'r') as f:
    tasks = json.load(f)

# Game-related tasks
game_tasks = [
    # Crossword tasks
    {
        "id": "crossword_challenge",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Complete today's crossword. No pressure, just play.",
        "placeholder": "How'd it go?"
    },
    {
        "id": "crossword_one_clue",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Try the crossword. Even solving one clue counts as a win.",
        "placeholder": "I got..."
    },
    {
        "id": "crossword_new_word",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play the crossword. Did you learn any new words?",
        "placeholder": "I learned..."
    },
    
    # Wordle tasks
    {
        "id": "wordle_daily",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Time for Wordle. What's your starting word strategy?",
        "placeholder": "I usually start with..."
    },
    {
        "id": "wordle_share",
        "type": "GAME",
        "categories": ["GAMES", "SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play Wordle. Share your grid (no spoilers!) with someone.",
        "placeholder": "I got it in..."
    },
    {
        "id": "wordle_brain_warm",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Warm up your brain with a quick Wordle.",
        "placeholder": "That was..."
    },
    
    # Pong / Arcade tasks
    {
        "id": "pong_quick_game",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play a quick round of Pong. Classic never dies.",
        "placeholder": "I scored..."
    },
    {
        "id": "pong_beat_score",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Try to beat your Pong high score. You've got this.",
        "placeholder": "My score was..."
    },
    {
        "id": "pong_zen_mode",
        "type": "GAME",
        "categories": ["GAMES", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play Pong for a few minutes. Just focus on the ball. Nothing else.",
        "placeholder": "That felt..."
    },
    
    # General game tasks
    {
        "id": "game_break",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Take a quick game break. Pick any game here and just play.",
        "placeholder": "I played..."
    },
    {
        "id": "game_childhood_memory",
        "type": "PROMPT",
        "categories": ["GAMES", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What game did you love playing as a kid? Why that one?",
        "placeholder": "I loved playing..."
    },
    {
        "id": "game_stress_relief",
        "type": "GAME",
        "categories": ["GAMES", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Stressed? Play a quick game. It's okay to take a break.",
        "placeholder": "After playing I feel..."
    },
    {
        "id": "game_focus_exercise",
        "type": "GAME",
        "categories": ["GAMES", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Games can sharpen focus. Pick one and give it your full attention.",
        "placeholder": "I noticed my focus..."
    },
    {
        "id": "game_just_for_fun",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "No goals, no scores to beat. Just play something for fun.",
        "placeholder": "That was..."
    },
    {
        "id": "game_new_strategy",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Try a game with a completely different strategy than usual.",
        "placeholder": "I tried..."
    },
    
    # Word game reflections
    {
        "id": "word_game_favorite",
        "type": "PROMPT",
        "categories": ["GAMES", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's a word you learned from a game that stuck with you?",
        "placeholder": "A word I learned..."
    },
    {
        "id": "word_play_creativity",
        "type": "PROMPT",
        "categories": ["GAMES", "CREATIVITY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Make up a word that should exist but doesn't. What does it mean?",
        "placeholder": "My made-up word is..."
    },
    
    # Competition/challenge tasks
    {
        "id": "game_personal_best",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Challenge yourself. Try to set a new personal best in any game.",
        "placeholder": "My new record..."
    },
    {
        "id": "game_three_rounds",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play three quick rounds of something. Best of three against yourself.",
        "placeholder": "Results:"
    },
    {
        "id": "game_losing_gracefully",
        "type": "PROMPT",
        "categories": ["GAMES", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Games teach us to lose gracefully. What's something you've 'lost' at lately that was actually okay?",
        "placeholder": "I lost at..."
    },
    
    # Mindful gaming
    {
        "id": "game_mindful_play",
        "type": "GAME",
        "categories": ["GAMES", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play a game slowly. Notice your breathing. Notice your reactions.",
        "placeholder": "I noticed..."
    },
    {
        "id": "game_before_bed",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "timeOfDay": "EVENING"
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Wind down with a relaxing game before bed.",
        "placeholder": "That helped me..."
    },
    {
        "id": "game_morning_wake",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "timeOfDay": "MORNING"
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Wake up your brain with a quick puzzle or word game.",
        "placeholder": "Now I feel..."
    },
    
    # Social gaming prompts
    {
        "id": "game_share_score",
        "type": "GAME",
        "categories": ["GAMES", "SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play a game and text someone your score. Challenge them to beat it.",
        "placeholder": "I challenged..."
    },
    {
        "id": "game_teach_someone",
        "type": "PROMPT",
        "categories": ["GAMES", "SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Think of someone who might enjoy a game you like. Would you teach them?",
        "placeholder": "I'd teach..."
    },
    
    # Puzzle reflection
    {
        "id": "puzzle_satisfaction",
        "type": "PROMPT",
        "categories": ["GAMES", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's satisfying about solving a puzzle?",
        "placeholder": "The satisfaction comes from..."
    },
    {
        "id": "puzzle_stuck",
        "type": "PROMPT",
        "categories": ["GAMES", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "When you're stuck on a puzzle, what's your approach? Does that apply to life problems too?",
        "placeholder": "When I'm stuck, I..."
    },
    
    # Quick hits
    {
        "id": "game_two_minutes",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "You have 2 minutes. Go play something. Now.",
        "placeholder": "Done!"
    },
    {
        "id": "game_celebrate_win",
        "type": "GAME",
        "categories": ["GAMES"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Play until you win something. Then actually celebrate it.",
        "placeholder": "I won!"
    }
]

# Add the new tasks
tasks.extend(game_tasks)

# Write back
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'w') as f:
    json.dump(tasks, f, indent=2)

print(f"Added {len(game_tasks)} game-related tasks. Total tasks: {len(tasks)}")
