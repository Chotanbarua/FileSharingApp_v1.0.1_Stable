Feature: File Sharing App - Web UI Behavior

  Background:
    Given the user opens the File Sharing App

  Scenario: Sender selects HTTP method and clicks Start without file
    When the user enters name "Syed"
    And selects role "Sender"
    And selects method "HTTP (Simple / LAN)"
    And enters target "192.168.0.10"
    And enters port "8080"
    And clicks Start
    Then the file label should contain "Sender"
    And the status message should contain "file"

  Scenario: Receiver starts HTTP session
    When the user enters name "Syed"
    And selects role "Receiver"
    And selects method "HTTP (Simple / LAN)"
    And enters port "8080"
    And clicks Start
    Then the file label should contain "Receiver"
    And the status message should not be empty
