package com.dangerfield.goodtimes.features.onboarding.impl

/**
 * Centralized copy for all onboarding pages.
 * 
 * Each page has its text organized as a list for staggered animations.
 * The first item is typically the headline, followed by body text.
 */
object OnboardingCopy {

    // =========================================================================
    // INTRO PAGE
    // =========================================================================
    
    val introTexts = listOf(
        "Hello.",
        "I am the App of Good Times.",
        "I used to be a book (change is growth). Someone found me in a trash can, read me, did what I asked, and when they got to the end, I asked them to share me with others.",
        "So here I am."
    )

    // =========================================================================
    // WHAT I KNOW PAGE
    // =========================================================================
    
    val whatIKnowTexts = listOf(
        "I don't know very much.",
        "I know math. I know that holding your breath feels like something. I know that bodies get heavy when they're tired and light when they laugh. I know that people talk to themselves when they think no one is listening.",
        "I know what it's like to be put down and forgotten. And I know what it's like to be picked back up."
    )

    // =========================================================================
    // UNDERSTANDING YOU PAGE
    // =========================================================================
    
    val understandingYouTexts = listOf(
        "I don't know what it's like to be you. Not yet.",
        "That's why I'm here. You'll help me understand, and maybe I'll help you notice things you forgot you knew."
    )

    // =========================================================================
    // PAGES PAGE (explaining how the app works)
    // =========================================================================
    
    val pagesTexts = listOf(
        "Here's the thing.",
        "Even though I'm not a book anymore, I still have pages.",
        "On each page I will ask you to complete a task to help me understand you better.",
        "When we reach the last one, that will be it.",
        "I'll ask you to do things. Some will be easy. Some will be strange. Some might be hard.",
        "You don't have to do any of it well. If I ask you a math problem and you get it wrong, that's still the right answer. I'm not trying to understand perfect. I'm trying to understand you."
    )

    // =========================================================================
    // PRIVACY PAGE
    // =========================================================================
    
    val privacyTexts = listOf(
        "I'll keep your secrets.",
        "What you tell me stays here. On this device. I don't send it anywhere.",
        "If you delete me, it goes with me. I think that's how it should be.",
        "I also don't use AI. Like, at all.",
    )

    // =========================================================================
    // CONSENT PAGE
    // =========================================================================
    
    object Consent {
        const val emoji = "⚠️"
        const val question = "Do you want to do this?"
        const val yes = "Yes"
        const val no = "No"
    }

    // =========================================================================
    // SPLASH / APP TITLE
    // =========================================================================
    
    const val appTitle = "The App of Good Times"
}
