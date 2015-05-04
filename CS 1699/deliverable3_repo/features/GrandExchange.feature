Feature:
  As a Runescape player
  I want to search for items on the Grand Exchange
  So that I can check item prices and trends

  Scenario:
    Given the Grand Exchange page
    When I click on the Market Movers tab
    And I select a category
    Then the top 100 items of that category are displayed

  Scenario:
    Given the Grand Exchange page
    When I click on the Catalogue tab
    And I select a category
    Then all items of that category are displayed

  Scenario:
    Given the Grand Exchange page
    When I search for an item
    Then all relevant search results are displayed

  Scenario:
    Given the Grand Exchange page
    When I click on the Item of The Week
    And I select a specific time frame
    Then the daily average price is displayed
    And the selected time frame average price is displayed

  Scenario:
    Given the Grand Exchange page
    When I search for an item
    And I filter results by price
    And by membership type
    Then the filtered results are displayed