Feature:
  As a Runescape player
  I want to search and browse the Game Guide
  So that I can look up entries about the game

  Scenario:
    Given the Skills Wiki page
    When I click on any skill icon
    Then that skill's main article shows up

  Scenario:
    Given the main Wiki page
    When I search for any game topic
    Then search results for that topic show up

  Scenario:
    Given the Quests Wiki page
    When I click on any quest category
    Then all quests that fall under a given category show up

  Scenario:
    Given the Gear Guide Wiki page
    When I select a combat method
    And I select a skill level
    Then all relevant combat gear is displayed

  Scenario:
    Given the Area Guides Wiki page
    When I click on a region
    And I click on a city or dungeon
    Then the area's article shows up