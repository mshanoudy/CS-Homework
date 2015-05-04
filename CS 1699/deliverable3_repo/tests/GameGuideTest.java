import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class GameGuideTest
{
    private WebDriver driver;

    @Before
    public void initialize()
    {
        driver = new FirefoxDriver();

        // Make sure to wait until page loads
        driver.manage().timeouts().implicitlyWait(45, TimeUnit.SECONDS);
    }

    @After
    public void finish()
    {
        driver.quit();
    }

    @Test
    public void testSkillPages ()
    {
        driver.get("http://services.runescape.com/m=rswiki/en/Skills?jptg=ia&jptv=navbar");

        driver.findElements(By.className("prettytable")).get(1)
              .findElement(By.tagName("td"))
              .findElement(By.className("wikiimg")).click();
        String headerName = driver.findElement(By.id("contents-header")).getText();

        assertEquals("Attack", headerName);
    }

    @Test
    public void testArticleSearch()
    {
        driver.get("http://services.runescape.com/m=rswiki/en/");

        driver.findElement(By.className("search"))
              .findElement(By.className("text")).sendKeys("runite ore");
        driver.findElement(By.className("search"))
              .findElement(By.tagName("form"))
              .findElements(By.tagName("input")).get(1).click();
        String headerName = driver.findElements(By.className("searchresult")).get(0).getText();

        assertEquals("Runite ore", headerName);
    }

    @Test
    public void testQuestCategories()
    {
        driver.get("http://services.runescape.com/m=rswiki/en/Quests?jptg=ia&jptv=navbar");

        driver.findElement(By.linkText("Novice Quests")).click();
        String headerName = driver.findElement(By.id("contents-header")).getText();

        assertEquals("Category:Novice Quests", headerName);

    }

    @Ignore
    @Test
    public void testCombatGear()
    {
        driver.get("http://services.runescape.com/m=itemdb_rs/gear_guide?jptg=ia&jptv=navbar");

    }

    @Test
    public void testAreaGuide()
    {
        driver.get("http://services.runescape.com/m=rswiki/en/Area_Guides?jptg=ia&jptv=navbar");

        driver.findElement(By.linkText("Asgarnia")).click();
        driver.findElement(By.linkText("Asgarnian Ice Dungeon")).click();
        String headerName = driver.findElement(By.id("contents-header")).getText();

        assertEquals("Asgarnian Ice Dungeon", headerName);
    }
}
