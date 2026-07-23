package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsGuiScaleRequirementTest {

    private final IsGuiScaleRequirement requirement = new IsGuiScaleRequirement();

    @Test
    void commaSeparatedEqualityConditionsAreAlternatives() {
        assertTrue(this.requirement.matchesGuiScaleConditions("2,3", 2.0D));
        assertTrue(this.requirement.matchesGuiScaleConditions("2,3", 3.0D));
    }

    @Test
    void singleEqualityConditionStillMatches() {
        assertTrue(this.requirement.matchesGuiScaleConditions("2", 2.0D));
    }

    @Test
    void equalityConditionsRejectNonMatchingScale() {
        assertFalse(this.requirement.matchesGuiScaleConditions("2,3", 4.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions("2", 3.0D));
    }

    @Test
    void asciiSpacesAreIgnored() {
        assertTrue(this.requirement.matchesGuiScaleConditions(" 2 , 3 ", 3.0D));
        assertTrue(this.requirement.matchesGuiScaleConditions(" > 1 , < 3 ", 2.0D));
    }

    @Test
    void malformedConditionsKeepLegacyFilteringBehavior() {
        assertFalse(this.requirement.isRequirementMet(null));
        assertFalse(this.requirement.isRequirementMet(""));
        assertFalse(this.requirement.isRequirementMet("invalid"));
        assertFalse(this.requirement.matchesGuiScaleConditions(null, 2.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions("", 2.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions("invalid,>=2,<=3", 2.0D));
        assertTrue(this.requirement.matchesGuiScaleConditions("invalid,2", 2.0D));
    }

    @Test
    void relationalOperatorsRemainStrictAndCumulative() {
        assertFalse(this.requirement.matchesGuiScaleConditions(">2", 2.0D));
        assertTrue(this.requirement.matchesGuiScaleConditions(">2", 2.0001D));
        assertFalse(this.requirement.matchesGuiScaleConditions("<2", 2.0D));
        assertTrue(this.requirement.matchesGuiScaleConditions("<2", 1.9999D));
        assertTrue(this.requirement.matchesGuiScaleConditions(">1,<3", 2.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions(">1,<3", 1.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions(">1,<3", 3.0D));
    }

    @Test
    void equalityAlternativesRemainConstrainedByRelationalConditions() {
        assertTrue(this.requirement.matchesGuiScaleConditions("2,3,>1,<4", 2.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions("2,3,>2,<4", 2.0D));
        assertFalse(this.requirement.matchesGuiScaleConditions("2,3,>1,<4", 2.5D));
    }

}
