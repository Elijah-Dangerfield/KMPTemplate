import json

# Read the existing tasks
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'r') as f:
    tasks = json.load(f)

gap_tasks = [
    # ==================== CREATIVITY (was only 1!) ====================
    {
        "id": "creativity_doodle_mood",
        "type": "DRAWING",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsDrawing": True},
        "instruction": "Doodle your current mood without thinking too hard.",
        "placeholder": ""
    },
    {
        "id": "creativity_dream_room",
        "type": "PROMPT",
        "categories": ["CREATIVITY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Describe your dream room. What's in it? What does it smell like?",
        "placeholder": "My dream room has..."
    },
    {
        "id": "creativity_six_word_story",
        "type": "PROMPT",
        "categories": ["CREATIVITY"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Write a six-word story about your life right now.",
        "placeholder": "Six words:"
    },
    {
        "id": "creativity_alternate_ending",
        "type": "PROMPT",
        "categories": ["CREATIVITY", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Think of something that happened today. Write an alternate ending.",
        "placeholder": "What if instead..."
    },
    {
        "id": "creativity_name_that_color",
        "type": "PROMPT",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Look around. Find a color and give it a brand new name.",
        "placeholder": "I call this color..."
    },
    {
        "id": "creativity_haiku_now",
        "type": "PROMPT",
        "categories": ["CREATIVITY"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Write a haiku about this exact moment. 5-7-5 syllables.",
        "placeholder": "Line 1 / Line 2 / Line 3"
    },
    {
        "id": "creativity_invent_holiday",
        "type": "PROMPT",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Invent a holiday. What's it called? How do people celebrate?",
        "placeholder": "My holiday is..."
    },
    {
        "id": "creativity_draw_invention",
        "type": "DRAWING",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsDrawing": True},
        "instruction": "Draw an invention that would make your life easier.",
        "placeholder": ""
    },
    {
        "id": "creativity_movie_title",
        "type": "PROMPT",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "If today were a movie, what would the title be?",
        "placeholder": "Today's movie title:"
    },
    {
        "id": "creativity_texture_story",
        "type": "PROMPT",
        "categories": ["CREATIVITY"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Touch something near you. Describe how it feels in the weirdest way possible.",
        "placeholder": "It feels like..."
    },
    {
        "id": "creativity_superhero_power",
        "type": "PROMPT",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "You get one superpower but it's incredibly specific. What is it?",
        "placeholder": "My very specific superpower is..."
    },
    {
        "id": "creativity_draw_future_self",
        "type": "DRAWING",
        "categories": ["CREATIVITY", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsDrawing": True},
        "instruction": "Draw yourself 5 years from now. What do you look like?",
        "placeholder": ""
    },
    {
        "id": "creativity_emoji_story",
        "type": "PROMPT",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Tell me about your day using only 5 emojis.",
        "placeholder": "My day in emojis:"
    },
    {
        "id": "creativity_letter_from_object",
        "type": "PROMPT",
        "categories": ["CREATIVITY"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Pick an object near you. Write a short letter from its perspective.",
        "placeholder": "Dear human..."
    },
    {
        "id": "creativity_new_constellation",
        "type": "DRAWING",
        "categories": ["CREATIVITY", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsDrawing": True},
        "instruction": "Draw a new constellation and name it.",
        "placeholder": ""
    },
    
    # ==================== WELLNESS (was only 4) ====================
    {
        "id": "wellness_body_scan",
        "type": "PROMPT",
        "categories": ["WELLNESS", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Quick body scan: where are you holding tension right now?",
        "placeholder": "I'm holding tension in..."
    },
    {
        "id": "wellness_water_check",
        "type": "ACTION",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Have you had enough water today? Go drink some now.",
        "placeholder": "Done! I feel..."
    },
    {
        "id": "wellness_posture_check",
        "type": "ACTION",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Check your posture. Roll your shoulders back. Unclench your jaw.",
        "placeholder": "Adjusted!"
    },
    {
        "id": "wellness_sleep_reflect",
        "type": "PROMPT",
        "categories": ["WELLNESS", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "How did you sleep last night? What affected it?",
        "placeholder": "Last night I slept..."
    },
    {
        "id": "wellness_energy_level",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Rate your energy right now 1-10. What would move it up one notch?",
        "placeholder": "My energy is... I could..."
    },
    {
        "id": "wellness_one_nice_thing",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's one nice thing you could do for your body today?",
        "placeholder": "I could..."
    },
    {
        "id": "wellness_screen_break",
        "type": "ACTION",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Look away from all screens. Focus on something 20 feet away for 20 seconds.",
        "placeholder": "I looked at..."
    },
    {
        "id": "wellness_meal_mindful",
        "type": "PROMPT",
        "categories": ["WELLNESS", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What was the last thing you ate? Did you actually taste it?",
        "placeholder": "I ate... and it tasted..."
    },
    {
        "id": "wellness_fresh_air",
        "type": "ACTION",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Open a window or step outside. Take 5 deep breaths of fresh air.",
        "placeholder": "That felt..."
    },
    {
        "id": "wellness_comfort_item",
        "type": "PROMPT",
        "categories": ["WELLNESS", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's something that always makes you feel physically comfortable?",
        "placeholder": "I feel comfortable when..."
    },
    {
        "id": "wellness_stress_where",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Where does stress show up in your body? Shoulders? Stomach? Somewhere else?",
        "placeholder": "I feel stress in my..."
    },
    {
        "id": "wellness_movement_joy",
        "type": "PROMPT",
        "categories": ["WELLNESS", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What type of movement actually feels good to you (not should, but does)?",
        "placeholder": "Movement I enjoy..."
    },
    {
        "id": "wellness_check_in_honest",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Honest check-in: How are you really doing today? Mind and body.",
        "placeholder": "Honestly, I'm..."
    },
    
    # ==================== ACTION (was only 7) ====================
    {
        "id": "action_dance_30sec",
        "type": "ACTION",
        "categories": ["ACTION", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Dance for 30 seconds. No one's watching. Or they are. Who cares.",
        "placeholder": "That was..."
    },
    {
        "id": "action_tidy_one_thing",
        "type": "ACTION",
        "categories": ["ACTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Tidy one small thing near you. Just one.",
        "placeholder": "I tidied..."
    },
    {
        "id": "action_text_someone",
        "type": "ACTION",
        "categories": ["ACTION", "SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Text someone you haven't talked to in a while. Just say hi.",
        "placeholder": "I texted..."
    },
    {
        "id": "action_walk_around",
        "type": "ACTION",
        "categories": ["ACTION", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Get up and walk around for 2 minutes. Anywhere counts.",
        "placeholder": "I walked..."
    },
    {
        "id": "action_compliment",
        "type": "ACTION",
        "categories": ["ACTION", "SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Give someone a genuine compliment today. In person or by message.",
        "placeholder": "I told someone..."
    },
    {
        "id": "action_change_location",
        "type": "ACTION",
        "categories": ["ACTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Move to a different spot. Different room, different chair, outside. Just shift.",
        "placeholder": "I moved to..."
    },
    {
        "id": "action_sing_something",
        "type": "ACTION",
        "categories": ["ACTION", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Sing a few lines of any song. Out loud. Volume optional.",
        "placeholder": "I sang..."
    },
    {
        "id": "action_make_your_bed",
        "type": "ACTION",
        "categories": ["ACTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "conditions": {"timeOfDay": "MORNING"},
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "If you haven't already, go make your bed. It takes 2 minutes.",
        "placeholder": "Done!"
    },
    {
        "id": "action_jump",
        "type": "ACTION",
        "categories": ["ACTION", "PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Jump up and down 10 times. Shake things loose.",
        "placeholder": "Now I feel..."
    },
    {
        "id": "action_send_photo",
        "type": "ACTION",
        "categories": ["ACTION", "SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Send someone a photo of something you see right now. No context needed.",
        "placeholder": "I sent a photo of..."
    },
    {
        "id": "action_hold_plank",
        "type": "ACTION",
        "categories": ["ACTION", "WELLNESS"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Hold a plank for as long as you can. How long did you last?",
        "placeholder": "I held it for..."
    },
    {
        "id": "action_smile_mirror",
        "type": "ACTION",
        "categories": ["ACTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Find a mirror. Smile at yourself. Mean it.",
        "placeholder": "That felt..."
    },
    {
        "id": "action_declutter_five",
        "type": "ACTION",
        "categories": ["ACTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Find 5 things you don't need. Put them in a donation pile.",
        "placeholder": "I found..."
    },
    
    # ==================== SOCIAL (was only 9) ====================
    {
        "id": "social_thinking_of_you",
        "type": "ACTION",
        "categories": ["SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Send a 'thinking of you' message to someone. That's it.",
        "placeholder": "I messaged..."
    },
    {
        "id": "social_who_needs_checkin",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Who in your life might need a check-in right now?",
        "placeholder": "I should check on..."
    },
    {
        "id": "social_gratitude_text",
        "type": "ACTION",
        "categories": ["SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Text someone and tell them one specific thing you appreciate about them.",
        "placeholder": "I told them I appreciate..."
    },
    {
        "id": "social_old_friend",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Who's an old friend you've lost touch with? What do you miss about them?",
        "placeholder": "I miss..."
    },
    {
        "id": "social_ask_for_help",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "What's something you could ask for help with? Who could you ask?",
        "placeholder": "I could ask for help with..."
    },
    {
        "id": "social_last_laugh",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "When did you last really laugh with someone? What happened?",
        "placeholder": "We laughed about..."
    },
    {
        "id": "social_recommend_something",
        "type": "ACTION",
        "categories": ["SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Recommend a song, show, or book to someone who'd love it.",
        "placeholder": "I recommended..."
    },
    {
        "id": "social_inside_joke",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's an inside joke you have with someone? (You don't have to explain it)",
        "placeholder": "The inside joke is..."
    },
    {
        "id": "social_learned_from",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Who's someone who taught you something important? Have you told them?",
        "placeholder": "I learned from..."
    },
    {
        "id": "social_safe_person",
        "type": "PROMPT",
        "categories": ["SOCIAL", "REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Who's someone you feel completely safe with? What makes them that person?",
        "placeholder": "I feel safe with..."
    },
    {
        "id": "social_voice_note",
        "type": "ACTION",
        "categories": ["SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Send someone a voice note instead of a text. It's more personal.",
        "placeholder": "I sent a voice note to..."
    },
    {
        "id": "social_memory_share",
        "type": "ACTION",
        "categories": ["SOCIAL"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Share a memory with someone who was there. 'Remember when...'",
        "placeholder": "I reminded them about..."
    },
    
    # ==================== STILLNESS (more breathing/mindfulness) ====================
    {
        "id": "stillness_breath_count",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Take 10 slow breaths. Count each exhale. Start now.",
        "placeholder": "After 10 breaths I feel..."
    },
    {
        "id": "stillness_one_minute_silence",
        "type": "WAIT_TIMER",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Sit in complete silence for one minute. Just be.",
        "placeholder": "That was..."
    },
    {
        "id": "stillness_notice_sounds",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Close your eyes. Name 5 different sounds you can hear.",
        "placeholder": "I heard..."
    },
    {
        "id": "stillness_feet_on_ground",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Feel your feet on the ground. Really feel them. Wiggle your toes.",
        "placeholder": "I notice..."
    },
    {
        "id": "stillness_box_breathing",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Box breathing: Breathe in 4 seconds, hold 4, out 4, hold 4. Repeat 4 times.",
        "placeholder": "After box breathing..."
    },
    {
        "id": "stillness_watch_clouds",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "If you can, look at the sky. Watch it for a full minute.",
        "placeholder": "The sky looked..."
    },
    {
        "id": "stillness_hands_rest",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Place your hands palm-down on your thighs. Feel their weight. Rest.",
        "placeholder": "That felt..."
    },
    {
        "id": "stillness_here_now",
        "type": "PROMPT",
        "categories": ["STILLNESS", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's true right here, right now, in this exact moment?",
        "placeholder": "Right now..."
    },
    {
        "id": "stillness_letting_thoughts_pass",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "For 2 minutes, notice your thoughts but don't follow them. Let them pass like clouds.",
        "placeholder": "I noticed thoughts about..."
    },
    {
        "id": "stillness_single_object",
        "type": "INSTRUCTION",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Pick one object. Look at it for 30 seconds like you've never seen it before.",
        "placeholder": "I noticed..."
    },
    
    # ==================== GRATITUDE (dedicated set) ====================
    {
        "id": "gratitude_three_things",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Name three things you're grateful for right now. Be specific.",
        "placeholder": "1. 2. 3."
    },
    {
        "id": "gratitude_body_part",
        "type": "PROMPT",
        "categories": ["REFLECTION", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's a body part you're grateful for? Why that one?",
        "placeholder": "I'm grateful for my..."
    },
    {
        "id": "gratitude_small_thing",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's a tiny thing you're grateful for that most people overlook?",
        "placeholder": "A small thing I appreciate..."
    },
    {
        "id": "gratitude_challenge",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "What's something difficult that you're secretly grateful for?",
        "placeholder": "I'm grateful for the challenge of..."
    },
    {
        "id": "gratitude_person_unsaid",
        "type": "PROMPT",
        "categories": ["REFLECTION", "SOCIAL"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "Who deserves your thanks but hasn't heard it? What would you say?",
        "placeholder": "I'd thank..."
    },
    {
        "id": "gratitude_past_self",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "What's something your past self did that you're now grateful for?",
        "placeholder": "Thanks to past me for..."
    },
    {
        "id": "gratitude_senses",
        "type": "PROMPT",
        "categories": ["REFLECTION", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's one thing you can see, hear, or smell right now that you appreciate?",
        "placeholder": "I appreciate..."
    },
    
    # ==================== MUSIC / AUDIO ====================
    {
        "id": "music_current_mood",
        "type": "PROMPT",
        "categories": ["PLAY", "REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What song matches your mood right now?",
        "placeholder": "My mood song is..."
    },
    {
        "id": "music_memory_song",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "What song instantly takes you back to a specific memory?",
        "placeholder": "When I hear... I remember..."
    },
    {
        "id": "music_listen_fully",
        "type": "ACTION",
        "categories": ["STILLNESS"],
        "difficulty": "MEDIUM",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Put on a song and actually listen to it. All of it. Nothing else.",
        "placeholder": "I listened to... and noticed..."
    },
    {
        "id": "music_hum_something",
        "type": "ACTION",
        "categories": ["PLAY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Hum the first song that comes to mind. What was it?",
        "placeholder": "I hummed..."
    },
    {
        "id": "music_soundtrack_life",
        "type": "PROMPT",
        "categories": ["PLAY", "CREATIVITY"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "MEDIUM"},
        "instruction": "If your life had a soundtrack, what would be playing right now?",
        "placeholder": "My life soundtrack is..."
    },
    {
        "id": "music_childhood_song",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What song reminds you of childhood?",
        "placeholder": "From my childhood..."
    },
    {
        "id": "audio_record_thought",
        "type": "AUDIO_CAPTURE",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsVoice": True},
        "instruction": "Record a quick voice note to yourself. What's on your mind?",
        "placeholder": ""
    },
    {
        "id": "sound_favorite",
        "type": "PROMPT",
        "categories": ["REFLECTION"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's your favorite sound? Not music - just a sound.",
        "placeholder": "My favorite sound is..."
    },
    
    # ==================== BODY / PHYSICAL awareness ====================
    {
        "id": "body_temperature",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Are you warm enough? Too warm? What would feel better?",
        "placeholder": "Right now I feel..."
    },
    {
        "id": "body_hands_doing",
        "type": "PROMPT",
        "categories": ["STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Look at your hands. What have they done today?",
        "placeholder": "My hands have..."
    },
    {
        "id": "body_face_tension",
        "type": "ACTION",
        "categories": ["WELLNESS", "STILLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Relax your forehead. Relax your jaw. Relax your tongue from the roof of your mouth.",
        "placeholder": "That felt..."
    },
    {
        "id": "body_shake_it",
        "type": "ACTION",
        "categories": ["ACTION", "WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Stand up and shake your whole body for 15 seconds. Get weird with it.",
        "placeholder": "Now I feel..."
    },
    {
        "id": "body_heart_rate",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "Put your hand on your chest. Feel your heartbeat. Is it fast or slow?",
        "placeholder": "My heartbeat is..."
    },
    {
        "id": "body_favorite_stretch",
        "type": "PROMPT",
        "categories": ["WELLNESS"],
        "difficulty": "LIGHT",
        "requiresSocial": False,
        "safeToReflect": True,
        "responseStyle": {"allowsText": True, "expectedLength": "SHORT"},
        "instruction": "What's your favorite stretch? Do it now.",
        "placeholder": "I stretched my..."
    }
]

# Add the new tasks
tasks.extend(gap_tasks)

# Write back
with open('libraries/goodtimes/impl/src/commonMain/composeResources/files/tasks.json', 'w') as f:
    json.dump(tasks, f, indent=2)

print(f"Added {len(gap_tasks)} gap-filling tasks. Total tasks: {len(tasks)}")
