package com.haraldsson.syntropy;

import com.haraldsson.syntropy.core.EventType;
import com.haraldsson.syntropy.core.GameEvents;
import com.haraldsson.syntropy.systems.ResearchSystem;
import com.haraldsson.syntropy.systems.Technology;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResearchSystemTest {

    @Test
    void firesResearchCompletedEventWhenTechFinishes() {
        // arrange — "basic_farming" has no prerequisites, researchTime=20f
        GameEvents events = new GameEvents();
        ResearchSystem research = new ResearchSystem(events);
        research.startResearch("basic_farming");

        List<Object> fired = new ArrayList<>();
        events.on(EventType.RESEARCH_COMPLETED, fired::add);

        // act — tick past the research time (20 seconds)
        research.update(21f);

        // assert
        assertFalse(fired.isEmpty(), "Expected RESEARCH_COMPLETED event to be fired");
        assertEquals("Basic Farming", fired.get(0));
    }

    @Test
    void cannotStartTechWithUnmetPrerequisite() {
        // arrange — "fast_mining" requires "basic_mining"
        GameEvents events = new GameEvents();
        ResearchSystem research = new ResearchSystem(events);

        // attempt to start fast_mining without completing basic_mining first
        research.startResearch("fast_mining");

        // assert — currentResearch should still be null (not started)
        assertNull(research.getCurrentResearch(),
                "Should not be able to start fast_mining before completing basic_mining");
    }

    @Test
    void canStartTechWhenPrerequisitesMet() {
        // arrange — complete basic_mining, then start fast_mining
        GameEvents events = new GameEvents();
        ResearchSystem research = new ResearchSystem(events);

        research.startResearch("basic_mining");
        research.update(25f); // basic_mining costs 20f — complete it

        assertTrue(research.isCompleted("basic_mining"), "basic_mining should be completed");

        // now start fast_mining
        research.startResearch("fast_mining");
        assertNotNull(research.getCurrentResearch(), "fast_mining should start after prerequisite is met");
        assertEquals("fast_mining", research.getCurrentResearch().getId());
    }

    @Test
    void startNextResearchSkipsTechsWithUnmetPrerequisites() {
        // arrange — first available ERA-1 techs have no prerequisites, so startNext should pick one
        GameEvents events = new GameEvents();
        ResearchSystem research = new ResearchSystem(events);

        research.startNextResearch();

        Technology current = research.getCurrentResearch();
        assertNotNull(current, "Should pick the first available tech");
        // All era-1 techs have no prerequisites
        assertEquals(1, current.getEra(), "Should pick an era-1 tech first");
        assertTrue(current.getPrerequisites().isEmpty(), "Selected tech should have no unmet prerequisites");
    }
}
