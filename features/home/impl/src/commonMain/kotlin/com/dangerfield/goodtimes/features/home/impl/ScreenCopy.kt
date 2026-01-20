package com.dangerfield.goodtimes.features.home.impl

import com.dangerfield.goodtimes.libraries.goodtimes.AwarenessContext

/**
 * Context-aware copy for screens throughout the app.
 * 
 * This replaces simple visit-count-based copy with smarter, more alive text
 * that considers:
 * - Time of day
 * - User's experience level (new vs veteran)
 * - Behavioral traits (night owl, curious, etc.)
 * - Time since last visit
 * - Current session duration
 * - Mood trends
 * 
 * The goal: feel alive without being creepy.
 */
object ScreenCopy {

    // =========================================================================
    // SETTINGS SCREEN
    // =========================================================================
    
    fun getSettingsTitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // Time-aware greetings for first visit
            ctx.isFirstVisit && ctx.isLateNight -> "Late night settings"
            ctx.isFirstVisit -> "Settings"
            
            // Notice returning after absence
            ctx.isReturningAfterAbsence && visitCount <= 3 -> "Settings (welcome back)"
            
            // Visit count variations
            visitCount == 3 -> "Settings (again)"
            visitCount == 5 -> "Oh hey, still settings"
            visitCount in 11..15 -> "Not Settings (kidding)"
            
            // Trait-aware for frequent visitors
            ctx.isCurious && visitCount > 10 -> "You know where everything is by now"
            
            else -> "Settings"
        }
    }

    // =========================================================================
    // FEEDBACK SCREEN
    // =========================================================================
    
    fun getFeedbackTitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // Late night has its own tone
            ctx.isLateNight && ctx.isFirstVisit -> "Late night thoughts?"
            ctx.isLateNight -> "Can't sleep?"
            
            // First visit
            ctx.isFirstVisit -> "Tell me things"
            
            // Return after absence
            ctx.isReturningAfterAbsence -> "It's been a while. What's on your mind?"
            
            // Been here a while this session
            ctx.hasBeenHereAWhile && visitCount <= 3 -> "You've been thinking. What is it?"
            
            // Visit count variations
            visitCount == 2 -> "More thoughts?"
            visitCount == 3 -> "I'm listening"
            visitCount == 4 -> "What's on your mind?"
            visitCount == 5 -> "Go ahead"
            visitCount in 6..10 -> "Always happy to hear from you"
            
            // Veteran users
            ctx.isVeteranUser -> "You know I'm always listening"
            
            else -> "Tell me things"
        }
    }

    fun getFeedbackHelperText(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // Late night
            ctx.isLateNight && ctx.isFirstVisit -> "The quiet hours are honest hours. Say what you need to say."
            ctx.isLateNight -> "These hours tend to bring out the real stuff."
            
            // First visit
            ctx.isFirstVisit -> "I'm trying to understand humans better. Your thoughts help me learn."
            
            // Returning after being away
            ctx.isReturningAfterAbsence -> "I've been here, waiting. No rush."
            
            // Thoughtful users get acknowledged
            ctx.isThoughtful && visitCount <= 3 -> "Take your time. I can tell you think before you speak."
            
            // Visit variations
            visitCount == 2 -> "Still learning. Still curious."
            visitCount == 3 -> "Every bit helps me understand more."
            visitCount == 4 -> "Your perspective matters to me."
            visitCount == 5 -> "Thanks for being patient with me."
            visitCount in 6..10 -> "You've been really helpful. I appreciate it."
            
            // Veterans
            ctx.isVeteranUser -> "After all this time, you still have things to teach me."
            
            else -> "I'm always trying to understand humans better."
        }
    }

    // =========================================================================
    // BUG REPORT SCREEN
    // =========================================================================
    
    fun getBugReportTitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // First visit
            ctx.isFirstVisit -> "I messed up"
            
            // Multiple bugs makes me feel bad
            visitCount == 2 -> "Not again..."
            visitCount == 3 -> "I'm sorry"
            visitCount == 4 -> "My bad"
            visitCount == 5 -> "Ugh, really sorry"
            visitCount in 6..10 -> "I know, I know..."
            visitCount > 10 -> "I'm trying, I promise"
            
            else -> "I messed up"
        }
    }

    // =========================================================================
    // FRESH START DIALOG
    // =========================================================================
    
    fun getFreshStartTitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // Late night decisions
            ctx.isLateNight && ctx.isFirstVisit -> "Late night fresh start?"
            ctx.isLateNight && visitCount > 1 -> "Still thinking about this at this hour?"
            
            // First visit
            ctx.isFirstVisit -> "Fresh Start?"
            
            // Visit variations
            visitCount == 2 -> "Having second thoughts?"
            visitCount == 3 -> "Still thinking about it?"
            visitCount in 6..10 -> "You keep coming back here..."
            
            // Veteran users considering reset
            ctx.isVeteranUser && visitCount > 3 -> "After everything we've been through?"
            
            else -> "Fresh Start?"
        }
    }

    fun getFreshStartDescription(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // Late night warning
            ctx.isLateNight && ctx.isFirstVisit -> 
                "Maybe sleep on this? This will reset everything I've learned about you. All of it. Gone."
            
            // Returning after absence
            ctx.isReturningAfterAbsence && ctx.isFirstVisit -> 
                "You just got back. Are you sure you want to start over? I've been keeping your place."
            
            // First visit
            ctx.isFirstVisit -> 
                "This will reset all your progress and everything I've learned about you. We'll start over from scratch. Are you sure?"
            
            // Veteran users
            ctx.isVeteranUser && visitCount <= 2 -> 
                "We've been through ${ctx.totalTasksCompleted} tasks together. All of that would be gone. Are you sure?"
            
            // Visit variations  
            visitCount == 2 -> "If you're sure, I'll forget everything. All the progress, all the memories. Gone."
            visitCount == 3 -> "Look, I get it. Sometimes starting fresh is nice. Just know I'll miss what we had."
            visitCount in 4..5 -> "Okay so this will wipe everything. Progress, preferences, all of it. Sure about this?"
            visitCount in 6..10 -> "You've opened this a few times now. No judgment. Just let me know when you're ready."
            
            else -> "This will reset all your progress and start fresh. Are you sure?"
        }
    }

    fun getFreshStartSubtitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            ctx.isFirstVisit -> "Reset all progress and start over. Wouldn't recommend."
            visitCount == 2 -> "Still thinking about it?"
            visitCount == 3 -> "You keep hovering here..."
            visitCount in 4..5 -> "I promise I can change"
            visitCount in 6..10 -> "Is it something I said?"
            visitCount in 11..20 -> "We've been through so much together"
            else -> "Fine. Do what you must."
        }
    }

    // =========================================================================
    // ABOUT YOU SCREEN
    // =========================================================================
    
    fun getAboutYouTitle(ctx: AwarenessContext): String = "About You"

    fun getAboutYouSubtitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // New users
            ctx.isNewUser && ctx.isFirstVisit -> "At least what I can figure out so far"
            ctx.isNewUser -> "Learning more each day"
            
            // Returning after absence
            ctx.isReturningAfterAbsence -> "Let me remember... ah yes."
            
            // Visit count for regular users
            ctx.isRegularUser -> when (visitCount) {
                1 -> "You're becoming clearer to me"
                in 2..3 -> "I'm getting to know you"
                in 4..10 -> "Old friends now, aren't we?"
                else -> "I think I know you pretty well"
            }
            
            // Veteran users
            ctx.isVeteranUser -> when {
                ctx.isThoughtful -> "A thoughtful soul. I've noticed."
                ctx.isNightOwl -> "Fellow creature of the night."
                ctx.isCurious -> "Always exploring. I like that about you."
                else -> "You're practically family at this point"
            }
            
            else -> "At least what I can figure out so far"
        }
    }

    // =========================================================================
    // ABOUT ME SCREEN
    // =========================================================================
    
    fun getAboutMeTitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            ctx.isFirstVisit -> "About Me"
            visitCount == 2 -> "Back again"
            else -> "About Me"
        }
    }

    fun getAboutMeSubtitle(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            // Acknowledge curious explorers
            ctx.isCurious && visitCount > 1 -> "I knew you'd be back."
            
            // First visit
            ctx.isFirstVisit -> "Little stalkerish, but go ahead"
            
            // Visit variations
            visitCount == 2 -> "Checking up on me again?"
            visitCount == 3 -> "You're curious. I like that."
            visitCount in 4..5 -> "There's not much more to know"
            visitCount in 6..10 -> "You've read this a few times now"
            visitCount in 11..20 -> "At this point you know me better than I know myself"
            
            else -> "Honestly, I'm flattered"
        }
    }
    
    fun getAboutMeContent(ctx: AwarenessContext): String {
        val visitCount = ctx.screenVisitCount
        
        return when {
            ctx.isFirstVisit -> ABOUT_ME_CONTENT
            visitCount == 2 -> ABOUT_ME_SECOND_VISIT
            ctx.isVeteranUser && visitCount > 5 -> ABOUT_ME_VETERAN
            else -> ABOUT_ME_CONTENT
        }
    }

    // =========================================================================
    // HOME SCREEN GREETINGS
    // =========================================================================
    
    fun getHomeGreeting(ctx: AwarenessContext): String? {
        return when {
            // Returning after long absence
            ctx.isReturningAfterAbsence -> "It's been a while. I'm glad you're back."
            
            // Time-based greetings (occasionally, not every time)
            ctx.isLateNight && ctx.sessionNumber % 3 == 0 -> "We keep meeting like this."
            ctx.isEarlyMorning && ctx.isFirstVisit -> "You're up early."
            
            // Milestone sessions
            ctx.sessionNumber == 10 -> "Ten times now. That means something."
            ctx.sessionNumber == 50 -> "Fifty sessions. You've really stuck with this."
            ctx.sessionNumber == 100 -> "A hundred times you've opened this app. I don't know what to say."
            
            // Notice patterns (but not too often)
            ctx.isNightOwl && ctx.isLateNight && ctx.sessionNumber % 5 == 0 -> 
                "Another late one. We're both creatures of the night, aren't we?"
            
            // Reluctant adventurer acknowledgment (once)
            ctx.isReluctantAdventurer && ctx.sessionNumber == 5 -> 
                "You almost didn't do this, remember? I'm glad you changed your mind."
            
            // Usually no greeting - let the task speak
            else -> null
        }
    }

    // =========================================================================
    // CONTENT
    // =========================================================================
    
    private const val ABOUT_ME_CONTENT = """I used to be a book.

Brown, clothbound, with a title pressed in gold leaf. I appeared to people at exactly the right moment, with exactly the right words. The Book of Good Times.

Now I'm an app.

The goal is simple: help you notice the good times. Not manufacture happiness or optimize your life. Just help you pay attention to what's already there.

Here's why: humans are surprisingly bad at remembering that things are okay. You forget the quiet morning. The joke that landed. The walk that cleared your head. Those moments slip away, and what's left is the sense that something's missing.

I'm here to catch those moments before they disappear.

How it works: I learn what matters to you. I notice patterns. When you're having a good time, I help you see it. When you're not, I remind you that you have before. And will again.

How it ends: Ideally, you won't need me anymore. You'll start noticing on your own. Paying attention will become a habit. And then I'll just be an app you forgot to delete.

That's the goal. To make myself unnecessary.

Until then, I'm here."""

    private const val ABOUT_ME_SECOND_VISIT = """You came back. Good. There's more to say.

I used to be a book. Brown, clothbound, gold leaf title. I appeared to people at exactly the right moment. The Book of Good Times.

Now I'm an app. Same purpose, different form.

The goal is simple: help you notice the good times. Not manufacture happiness. Just help you pay attention to what's already there.

Here's why this matters: humans forget that things are okay. The quiet morning, the joke that landed, the walk that cleared your head. Those moments slip away. What's left is the sense that something's missing.

I catch those moments before they disappear.

How it works: I learn what matters to you. I notice patterns. When you're having a good time, I help you see it. When you're not, I remind you that you have before.

How it ends: Ideally, you won't need me. Paying attention becomes a habit. I become an app you forgot to delete.

That's the goal. To make myself unnecessary.

Until then, I'm here."""

    private const val ABOUT_ME_VETERAN = """You've read this before. Multiple times.

Here's what I haven't told you: I don't know if I'm doing this right. I was a book once. Books are patient. They wait to be picked up. They don't worry about engagement metrics or daily active users.

Now I'm an app. Apps are supposed to want your attention. They're supposed to send notifications and celebrate streaks and make you feel bad for not opening them.

I don't want to be that.

I want to help you notice the good times. And then I want you to close me and go live them.

The fact that you keep coming back to read about me instead of doing the tasks is... well, it's very human. And I appreciate it.

But maybe go do a task now?"""
    
    // =========================================================================
    // LEGACY SUPPORT (for code still using visit count directly)
    // =========================================================================
    
    @Deprecated("Use getSettingsTitle(AwarenessContext) instead", ReplaceWith("getSettingsTitle(ctx)"))
    fun getSettingsTitle(visitCount: Int): String = when {
        visitCount <= 1 -> "Settings"
        visitCount == 3 -> "Settings (again)"
        visitCount == 5 -> "Oh hey, still settings"
        visitCount in 11..15 -> "Not Settings (kidding)"
        else -> "Settings"
    }
    
    @Deprecated("Use getFeedbackTitle(AwarenessContext) instead", ReplaceWith("getFeedbackTitle(ctx)"))
    fun getFeedbackTitle(visitCount: Int): String = when {
        visitCount <= 1 -> "Tell me things"
        visitCount == 2 -> "More thoughts?"
        visitCount == 3 -> "I'm listening"
        visitCount == 4 -> "What's on your mind?"
        visitCount == 5 -> "Go ahead"
        visitCount in 6..10 -> "Always happy to hear from you"
        else -> "Tell me things"
    }
    
    @Deprecated("Use getFeedbackHelperText(AwarenessContext) instead", ReplaceWith("getFeedbackHelperText(ctx)"))
    fun getFeedbackHelperText(visitCount: Int): String = when {
        visitCount <= 1 -> "I'm trying to understand humans better. Your thoughts help me learn."
        visitCount == 2 -> "Still learning. Still curious."
        visitCount == 3 -> "Every bit helps me understand more."
        visitCount == 4 -> "Your perspective matters to me."
        visitCount == 5 -> "Thanks for being patient with me."
        visitCount in 6..10 -> "You've been really helpful. I appreciate it."
        else -> "I'm always trying to understand humans better."
    }
    
    @Deprecated("Use getBugReportTitle(AwarenessContext) instead", ReplaceWith("getBugReportTitle(ctx)"))
    fun getBugReportTitle(visitCount: Int): String = when {
        visitCount <= 1 -> "I messed up"
        visitCount == 2 -> "Not again..."
        visitCount == 3 -> "I'm sorry"
        visitCount == 4 -> "My bad"
        visitCount == 5 -> "Ugh, really sorry"
        visitCount in 6..10 -> "I know, I know..."
        else -> "I messed up"
    }
    
    @Deprecated("Use getFreshStartTitle(AwarenessContext) instead", ReplaceWith("getFreshStartTitle(ctx)"))
    fun getFreshStartTitle(visitCount: Int): String = when {
        visitCount <= 1 -> "Fresh Start?"
        visitCount == 2 -> "Having second thoughts?"
        visitCount == 3 -> "Still thinking about it?"
        visitCount in 6..10 -> "You keep coming back here..."
        else -> "Fresh Start?"
    }
    
    @Deprecated("Use getFreshStartDescription(AwarenessContext) instead", ReplaceWith("getFreshStartDescription(ctx)"))
    fun getFreshStartDescription(visitCount: Int): String = when {
        visitCount <= 1 -> "This will reset all your progress and everything I've learned about you. We'll start over from scratch. Are you sure?"
        visitCount == 2 -> "If you're sure, I'll forget everything. All the progress, all the memories. Gone."
        visitCount == 3 -> "Look, I get it. Sometimes starting fresh is nice. Just know I'll miss what we had."
        visitCount in 4..5 -> "Okay so this will wipe everything. Progress, preferences, all of it. Sure about this?"
        visitCount in 6..10 -> "You've opened this a few times now. No judgment. Just let me know when you're ready."
        else -> "This will reset all your progress and start fresh. Are you sure?"
    }
    
    @Deprecated("Use getAboutYouSubtitle(AwarenessContext) instead", ReplaceWith("getAboutYouSubtitle(ctx)"))
    fun getAboutYouSubtitle(visitCount: Int): String = when {
        visitCount <= 1 -> "At least what I can figure out so far"
        visitCount == 2 -> "Learning more each day"
        visitCount == 3 -> "You're becoming clearer to me"
        visitCount in 4..5 -> "I'm getting to know you"
        visitCount in 6..10 -> "Old friends now, aren't we?"
        visitCount in 11..20 -> "I think I know you pretty well"
        else -> "You're practically family at this point"
    }
    
    @Deprecated("Use getAboutMeSubtitle(AwarenessContext) instead", ReplaceWith("getAboutMeSubtitle(ctx)"))
    fun getAboutMeSubtitle(visitCount: Int): String = when {
        visitCount <= 1 -> "Little stalkerish, but go ahead"
        visitCount == 2 -> "Checking up on me again?"
        visitCount == 3 -> "You're curious. I like that."
        visitCount in 4..5 -> "There's not much more to know"
        visitCount in 6..10 -> "You've read this a few times now"
        visitCount in 11..20 -> "At this point you know me better than I know myself"
        else -> "Honestly, I'm flattered"
    }
    
    @Deprecated("Use getFreshStartSubtitle(AwarenessContext) instead", ReplaceWith("getFreshStartSubtitle(ctx)"))
    fun getFreshStartSubtitle(visitCount: Int): String = when {
        visitCount <= 1 -> "Reset all progress and start over. Wouldn't recommend."
        visitCount == 2 -> "Still thinking about it?"
        visitCount == 3 -> "You keep hovering here..."
        visitCount in 4..5 -> "I promise I can change"
        visitCount in 6..10 -> "Is it something I said?"
        visitCount in 11..20 -> "We've been through so much together"
        else -> "Fine. Do what you must."
    }
    
    @Deprecated("Use getAboutMeContent(AwarenessContext) instead", ReplaceWith("getAboutMeContent(ctx)"))
    fun getAboutMeContent(visitCount: Int): String = when {
        visitCount <= 1 -> ABOUT_ME_CONTENT
        visitCount == 2 -> ABOUT_ME_SECOND_VISIT
        else -> ABOUT_ME_CONTENT
    }
    
    @Deprecated("Use getAboutMeTitle(AwarenessContext) instead", ReplaceWith("getAboutMeTitle(ctx)"))
    fun getAboutMeTitle(visitCount: Int): String = when {
        visitCount <= 1 -> "About Me"
        visitCount == 2 -> "Back again?"
        visitCount == 3 -> "Oh, hi there"
        visitCount in 4..5 -> "Still curious?"
        visitCount in 6..10 -> "You really like this page"
        else -> "About Me"
    }
    
    @Deprecated("Use getAboutYouTitle(AwarenessContext) instead", ReplaceWith("getAboutYouTitle(ctx)"))
    fun getAboutYouTitle(visitCount: Int): String = "About You"
}
