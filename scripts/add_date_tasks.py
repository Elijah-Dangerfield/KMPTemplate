import json

# Read the existing tasks
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'r') as f:
    tasks = json.load(f)

# Date-aware tasks to add
date_tasks = [
    # End of Year Reflection (December)
    {
        "id": "year_end_reflection",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "LONG"
        },
        "instruction": "As the year winds down, what moment from this year would you want to remember forever?",
        "placeholder": "This year, I'll always remember..."
    },
    {
        "id": "year_lessons_learned",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What's something this year taught you that you didn't expect to learn?",
        "placeholder": "I learned..."
    },
    {
        "id": "year_end_gratitude",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Who made your year better? Send them a thank you message right now if you can.",
        "placeholder": "This person made my year better..."
    },
    {
        "id": "year_end_letter_to_future",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "DEEP",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 12},
            "dayOfMonthRange": {"startDay": 26, "endDay": 31}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "LONG"
        },
        "instruction": "Write a note to yourself to read next December. What do you hope will be true?",
        "placeholder": "Dear future me..."
    },
    {
        "id": "year_end_letting_go",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "DEEP",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What are you ready to leave behind in this year?",
        "placeholder": "I'm ready to let go of..."
    },
    
    # New Year (Late December through early January)
    {
        "id": "new_year_intentions",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 1},
            "dayOfMonthRange": {"startDay": 28, "endDay": 7}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Forget resolutions. What's one word you want to guide your new year?",
        "placeholder": "My word for the new year is..."
    },
    {
        "id": "new_year_first_week",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 1, "endMonth": 1},
            "dayOfMonthRange": {"startDay": 1, "endDay": 7}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Happy new year! How does a fresh start feel right now?",
        "placeholder": "A fresh start feels..."
    },
    {
        "id": "january_fresh_energy",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 1, "endMonth": 1}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "January energy can be hopeful or heavy. Which is it for you right now?",
        "placeholder": "Right now January feels..."
    },
    {
        "id": "january_small_step",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 1, "endMonth": 1}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's one tiny step you could take today toward something you want?",
        "placeholder": "One small step I could take..."
    },
    
    # First of Month (any month)
    {
        "id": "first_of_month_intention",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "dayOfMonthRange": {"startDay": 1, "endDay": 3}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "New month, new page. What's one thing you want to make happen this month?",
        "placeholder": "This month I want to..."
    },
    {
        "id": "end_of_month_reflection",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "dayOfMonthRange": {"startDay": 28, "endDay": 31}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "The month is almost over. What surprised you about it?",
        "placeholder": "This month surprised me with..."
    },
    
    # Winter (December - February)
    {
        "id": "winter_cozy_moment",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 2}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "allowsPhoto": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's bringing you warmth today (literally or figuratively)?",
        "placeholder": "What's warming me is..."
    },
    {
        "id": "winter_hibernation_check",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 2}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Winter can be a time to slow down. What are you resting from?",
        "placeholder": "I'm resting from..."
    },
    {
        "id": "winter_indoor_joy",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 12, "endMonth": 2}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's something you enjoy that's best done indoors?",
        "placeholder": "I enjoy..."
    },
    
    # Spring (March - May)
    {
        "id": "spring_new_beginning",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 3, "endMonth": 5}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Spring is about new growth. What's trying to bloom in your life right now?",
        "placeholder": "What's growing in me is..."
    },
    {
        "id": "spring_clean_mind",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 3, "endMonth": 5}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Time for a mental spring cleaning. What thought pattern could you let go of?",
        "placeholder": "I could let go of thinking..."
    },
    {
        "id": "spring_outside",
        "type": "ACTION",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 3, "endMonth": 5}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "allowsPhoto": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Go outside for 5 minutes. Notice something that's changed since winter.",
        "placeholder": "I noticed..."
    },
    
    # Summer (June - August)
    {
        "id": "summer_adventure",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 6, "endMonth": 8}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What's your ideal summer day look like? Have you had one yet?",
        "placeholder": "My ideal summer day..."
    },
    {
        "id": "summer_memory",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 6, "endMonth": 8}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What's a favorite summer memory from when you were younger?",
        "placeholder": "I remember..."
    },
    {
        "id": "summer_slow_down",
        "type": "ACTION",
        "categories": ["WELLNESS", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 6, "endMonth": 8}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "Summer energy can be intense. Take 2 minutes to just... be still. What do you notice?",
        "placeholder": "I notice..."
    },
    
    # Fall (September - November)  
    {
        "id": "fall_letting_go",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 9, "endMonth": 11}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Trees release their leaves in fall. What could you release?",
        "placeholder": "I could release..."
    },
    {
        "id": "fall_gratitude_harvest",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 9, "endMonth": 11}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Fall is harvest season. What have you 'harvested' from your efforts this year?",
        "placeholder": "I've harvested..."
    },
    {
        "id": "fall_cozy_ritual",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 9, "endMonth": 11}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "SHORT"
        },
        "instruction": "What's your favorite cozy fall ritual?",
        "placeholder": "My cozy ritual is..."
    },
    
    # Holiday Season (November - December)
    {
        "id": "holiday_boundary",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 11, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Holiday season can be overwhelming. What's one boundary you need to protect?",
        "placeholder": "I need to protect..."
    },
    {
        "id": "holiday_meaning",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 11, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Beyond the commercial stuff, what does this time of year actually mean to you?",
        "placeholder": "This time of year means..."
    },
    {
        "id": "holiday_tradition",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 11, "endMonth": 12}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "What's a tradition (old or new) that actually feels meaningful to you?",
        "placeholder": "A meaningful tradition is..."
    },
    
    # February (Valentine's / Self-love month)
    {
        "id": "february_self_love",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 2, "endMonth": 2}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Forget romantic love for a second. What's one way you could show yourself more love?",
        "placeholder": "I could show myself love by..."
    },
    {
        "id": "february_love_letter_self",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "DEEP",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 2, "endMonth": 2}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "LONG"
        },
        "instruction": "Write yourself a short love letter. What would you want to hear?",
        "placeholder": "Dear me..."
    },
    {
        "id": "february_connection",
        "type": "PROMPT",
        "categories": ["SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 2, "endMonth": 2}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "Who's someone who makes you feel seen? Have you told them lately?",
        "placeholder": "Someone who sees me is..."
    },
    
    # Mid-year check-in (June-July)
    {
        "id": "midyear_checkin",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 6, "endMonth": 7},
            "dayOfMonthRange": {"startDay": 15, "endDay": 15}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "We're about halfway through the year. How's it going, honestly?",
        "placeholder": "Honestly, this year has been..."
    },
    {
        "id": "midyear_course_correct",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "conditions": {
            "monthRange": {"startMonth": 6, "endMonth": 7}
        },
        "safeToReflect": True,
        "responseStyle": {
            "allowsText": True,
            "expectedLength": "MEDIUM"
        },
        "instruction": "If you could course-correct one thing in the second half of this year, what would it be?",
        "placeholder": "I'd like to change..."
    }
]

# Add the new tasks
tasks.extend(date_tasks)

# Write back
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'w') as f:
    json.dump(tasks, f, indent=2)

print(f"Added {len(date_tasks)} date-aware tasks. Total tasks: {len(tasks)}")
