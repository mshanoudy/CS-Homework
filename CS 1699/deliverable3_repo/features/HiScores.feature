Feature:
  As a Runescape player
  I want to browse the HiScores
  So that I can see and compare the top players

  Scenario:
    Given the HiScores page
    And a valid player's name
    When I click on the Skills tab
    And I enter that player's name in the Player Name field
    And I click the go button
    Then that player's HiScores show up

  Scenario:
    Given the HiScores page
    And a valid player's name
    And a valid second player's name
    When I click on the Skills tab
    And I enter the first player's name in the Player 1 field
    And I enter the second player's name in the Player 2 field
    And I click the go button
    Then a comparison of each players' HiScores shows up

  Scenario:
    Given the HiScores page
    When I click on the Skills tab
    Then the overall HiScores show up

  Scenario:
    Given the HiScores page
    When I click on the Skills tab
    And I select any skill from the Skills dropdown
    Then the all-time HiScores for that skill show up

  Scenario:
    Given the HiScores page
    When I click on the Seasonal tab
    Then the Seasonal HiScores show up
