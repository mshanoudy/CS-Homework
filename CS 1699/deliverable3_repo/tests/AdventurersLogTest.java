import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class AdventurersLogTest
{
    private WebDriver driver;

    @Before
    public void initialize()
    {
        driver = new FirefoxDriver();

        // Adventurer's Log page
        driver.get("http://services.runescape.com/m=adventurers-log/a=13/landing");

        // Make sure to wait until page loads
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        // Navigate to my player page
        driver.findElement(By.id("searchName")).sendKeys("Don Gerrous");
        driver.findElement(By.className("advlog-search__submit")).click();
    }

    @After
    public void finish()
    {
        driver.quit();
    }

    @Test
    public void testProfile()
    {
        String headerName = driver.findElement(By.className("contents"))
                                  .findElement(By.tagName("h1")).getText();

        assertEquals("Don Gerrous", headerName);
    }

    @Test
    public void testSkillsTab()
    {
        driver.findElements(By.className("tabbednav__link")).get(1).click();
        String headerName = driver.findElement(By.className("single-title")).getText();

        assertEquals("Skills", headerName);
    }

    @Test
    public void testQuestsTab()
    {
        driver.findElements(By.className("tabbednav__link")).get(2).click();
        String headerName = driver.findElement(By.className("single-title")).getText();

        assertEquals("Quests", headerName);
    }

    @Test
    public void testActivityFeed()
    {
        driver.findElements(By.className("tabbednav__link")).get(3).click();
        String headerName = driver.findElement(By.className("single-title")).getText();

        assertEquals("Activity", headerName);
    }

    @Test
    public void testSkillStats() throws Exception
    {
        driver.findElements(By.className("tabbednav__link")).get(1).click();

        Actions actions = new Actions(driver);
        actions.pause(100); // Depreciated but don't really know how to get around this
        actions.clickAndHold(driver.findElements(By.className("skill")).get(0));
        actions.pause(100);
        actions.release();
        actions.build().perform();

        String headerName = driver.findElements(By.className("skill")).get(0)
                                  .findElement(By.tagName("h3")).getAttribute("innerHTML");

        assertEquals("Constitution Stats", headerName);
    }


}
