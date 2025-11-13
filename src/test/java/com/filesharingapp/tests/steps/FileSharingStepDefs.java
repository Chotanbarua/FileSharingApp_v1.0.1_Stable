package com.filesharingapp.tests.steps;

import com.filesharingapp.tests.base.TestBase;
import com.filesharingapp.tests.pages.IndexPage;
import io.cucumber.java.en.*;
import org.testng.Assert;

/**
 * FileSharingStepDefs
 * -------------------
 * This class connects the plain English steps (from the .feature file)
 * to Java code using the IndexPage (Page Object).
 *
 * IMPORTANT RULES:
 *   - This class does NOT extend TestBase.
 *   - This class does NOT create new IndexPage().
 *   - It always uses TestBase.indexPage() and TestBase.getDriver().
 *
 * Think of TestBase as the "framework brain" and this class as a
 * "translator" that only calls the brain when needed.
 */
public class FileSharingStepDefs {

    /**
     * Small helper method:
     *   Instead of writing TestBase.indexPage() everywhere,
     *   we write page() which is shorter and easier to read.
     */
    private IndexPage page() {
        return TestBase.indexPage();
    }

    @Given("the user opens the File Sharing App")
    public void user_opens_app() {
        // The browser and URL are already opened by CucumberHooks + TestBase.
        // Here we only assert that the page title looks correct.
        String title = TestBase.getDriver().getTitle().toLowerCase();

        Assert.assertTrue(
                title.contains("file") || title.contains("sharing"),
                "Page title should look like a File Sharing app. Actual: " + title
        );
    }

    @When("the user enters name {string}")
    public void user_enters_name(String name) {
        page().setName(name);
    }

    @When("selects role {string}")
    public void selects_role(String role) {
        page().chooseRole(role);
    }

    @When("selects method {string}")
    public void selects_method(String method) {
        page().chooseMethod(method);
    }

    @When("enters target {string}")
    public void enters_target(String target) {
        page().setTarget(target);
    }

    @When("enters port {string}")
    public void enters_port(String port) {
        page().setPort(port);
    }

    @When("clicks Start")
    public void clicks_start() {
        page().clickStart();
    }

    @Then("the file label should contain {string}")
    public void file_label_should_contain(String text) {
        String label = page().fileLabelText();
        Assert.assertTrue(
                label.toLowerCase().contains(text.toLowerCase()),
                "Expected file label to contain: " + text + " but was: " + label
        );
    }

    @Then("the status message should contain {string}")
    public void status_should_contain(String text) {
        String status = page().statusText();
        Assert.assertTrue(
                status.toLowerCase().contains(text.toLowerCase()),
                "Expected status to contain: " + text + " but was: " + status
        );
    }

    @Then("the status message should not be empty")
    public void status_not_empty() {
        String status = page().statusText();
        Assert.assertFalse(status.isBlank(), "Status should not be empty.");
    }
}
