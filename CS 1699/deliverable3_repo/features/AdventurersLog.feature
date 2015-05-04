Feature:
  As a Runescape player
  I want to view my Adventurer's Log
  So that I can track my character's progress

  Scenario:
    Given the Adventurer's Log page
    And my character's player name
    When I search for my account
    Then my character's profile shows up

  Scenario:
    Given the Adventurer's Log page
    And my character's player name
    When I search for my account
    And I click on the Skills tab
    Then my character's current skill levels and goals show up

  Scenario:
    Given the Adventurer's Log page
    And my character's player name
    When I search for my account
    And I click on the Quests tab
    Then my character's quest completion progress shows up

  Scenario:
    Given the Adventurer's Log page
    And my character's player name
    When I search for my account
    And I click on the Activity tab
    Then my character's recent activity feed shows up

  Scenario:
    Given the Adventurer's Log page
    And my character's player name
    When I search for my account
    And I click on the Skills tab
    And I click on any skill
    Then my character's stats for that skill show up