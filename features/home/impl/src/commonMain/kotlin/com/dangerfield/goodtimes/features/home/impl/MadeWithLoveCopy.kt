package com.dangerfield.goodtimes.features.home.impl

/**
 * Generates teasing messages for the "Made with ❤️" click easter egg.
 * These messages encourage users to keep clicking toward 100.
 */
object MadeWithLoveCopy {
    
    fun getMessage(clickCount: Int): String? = when (clickCount) {
        2 -> "Oh, we're doing this?"
        3 -> "Okay, I see you..."
        5 -> "Still going, huh?"
        7 -> "Most people stop by now"
        10 -> "Double digits! Impressive"
        15 -> "You're committed, I'll give you that"
        20 -> "Just a few more to go"
        25 -> "Getting closer..."
        30 -> "Halfway there!"
        35 -> "Did I say half? I meant less than half"
        40 -> "This is fun for me"
        45 -> "I could do this all day"
        50 -> "Okay now I'm actually impressed"
        55 -> "Your thumb must be tired"
        60 -> "Maybe take a break?"
        65 -> "No? Okay then"
        70 -> "30 more to go..."
        75 -> "You're in the home stretch"
        80 -> "So close I can taste it"
        85 -> "Almost... there..."
        90 -> "10 more. TEN."
        91 -> "NINE"
        92 -> "EIGHT"
        93 -> "SEVEN"
        94 -> "SIX"
        95 -> "FIVE MORE"
        97 -> "Three..."
        98 -> "Two..."
        99 -> "One..."
        100 -> null // Will trigger the dialog instead
        else -> null
    }
}
