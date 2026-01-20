import json

# Read the existing tasks
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'r') as f:
    tasks = json.load(f)

# Fixed sequence intro tasks that are missing
intro_tasks = [
    {
        "id": "first_task",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Look around you. What's one thing you notice right now?",
        "placeholder": "I notice...",
        "isIntroTask": True
    },
    {
        "id": "light_or_deep",
        "type": "SELECTION",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsSelection": True
        },
        "instruction": "Today, do you want to go light or deep?",
        "selectionOptions": ["Light", "Deep", "Surprise me"],
        "isIntroTask": True
    },
    {
        "id": "hold_finger_30",
        "type": "HOLD_FINGER",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Hold your finger on the screen for 30 seconds. Just... be here.",
        "placeholder": "That was...",
        "durationSeconds": 30,
        "isIntroTask": True
    },
    {
        "id": "three_sounds",
        "type": "PROMPT",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Close your eyes for a moment. What three sounds can you hear?",
        "placeholder": "I hear...",
        "isIntroTask": True
    },
    {
        "id": "favorite_word",
        "type": "PROMPT",
        "categories": ["PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's your favorite word? Any word. Why that one?",
        "placeholder": "My favorite word is...",
        "isIntroTask": True
    },
    {
        "id": "draw_turtle_dark",
        "type": "DRAWING",
        "categories": ["PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsDrawing": True
        },
        "instruction": "Draw a turtle. In the dark. (Close your eyes while you draw.)",
        "isIntroTask": True
    },
    {
        "id": "stranger_or_alone",
        "type": "SELECTION",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsSelection": True
        },
        "instruction": "Would you rather: be stuck in an elevator with a stranger, or completely alone?",
        "selectionOptions": ["With a stranger", "Alone"],
        "isIntroTask": True
    },
    {
        "id": "photo_something_old",
        "type": "PHOTO_CAPTURE",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsPhoto": True
        },
        "instruction": "Take a photo of something old near you.",
        "isIntroTask": True
    },
    {
        "id": "describe_sky",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Describe the sky right now. Or if you can't see it, describe the last sky you remember.",
        "placeholder": "The sky is...",
        "isIntroTask": True
    },
    {
        "id": "grateful_for_small",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Name one small thing you're grateful for today.",
        "placeholder": "I'm grateful for...",
        "isIntroTask": True
    },
    {
        "id": "hum_something",
        "type": "AUDIO_CAPTURE",
        "categories": ["PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsVoice": True
        },
        "instruction": "Hum the first song that comes to mind. Just a few seconds.",
        "isIntroTask": True
    },
    {
        "id": "photo_your_hands",
        "type": "PHOTO_CAPTURE",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsPhoto": True
        },
        "instruction": "Take a photo of your hands. Just as they are.",
        "isIntroTask": True
    },
    {
        "id": "stillness_60",
        "type": "STILLNESS",
        "categories": ["STILLNESS"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Hold completely still for 60 seconds. See if you can.",
        "placeholder": "That was...",
        "durationSeconds": 60,
        "isIntroTask": True
    },
    {
        "id": "draw_your_mood",
        "type": "DRAWING",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsDrawing": True
        },
        "instruction": "Draw your current mood. Abstract is fine.",
        "isIntroTask": True
    },
    {
        "id": "used_to_be",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What's something you used to be that you're not anymore?",
        "placeholder": "I used to be...",
        "isIntroTask": True
    }
]

# Add the intro tasks at the beginning (after intro_welcome which should be first)
# Find intro_welcome index
intro_idx = next((i for i, t in enumerate(tasks) if t['id'] == 'intro_welcome'), 0)

# Insert after intro_welcome
for i, task in enumerate(intro_tasks):
    tasks.insert(intro_idx + 1 + i, task)

# Write back
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'w') as f:
    json.dump(tasks, f, indent=2)

print(f"Added {len(intro_tasks)} intro sequence tasks. Total tasks: {len(tasks)}")
