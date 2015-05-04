import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

public class GrandExchangeTest
{
    private WebDriver driver;

    @Before
    public void initialize()
    {
        driver = new FirefoxDriver();

        // Make sure to wait until page loads
        driver.manage().timeouts().implicitlyWait(45, TimeUnit.SECONDS);

        driver.get("http://services.runescape.com/m=itemdb_rs/?jptg=ia&jptv=navbar");
    }

    @After
    public void finish()
    {
        driver.quit();
    }

    @Test
    public void testMarketMovers()
    {
        driver.findElement(By.className("secondary"))
              .findElements(By.tagName("li")).get(1)
              .findElement(By.tagName("span")).click();
        String headerName = driver.findElement(By.className("content"))
                                  .findElement(By.tagName("p")).getText();

        assertEquals("Top 100 most traded items", headerName);
    }

    @Test
    public void testCatalogue()
    {
        driver.findElement(By.className("secondary"))
              .findElements(By.tagName("li")).get(2)
              .findElement(By.tagName("span")).click();

        Actions actions = new Actions(driver);
        actions.moveToElement(driver.findElement(By.className("categoryDrop")));
        actions.click(driver.findElement(By.className("categories")).findElements(By.tagName("li")).get(0).findElement(By.tagName("a")));
        actions.build().perform();

        String headerName = driver.findElement(By.tagName("table"))
                                  .findElement(By.className("itemCol")).getText();

        assertEquals("Item", headerName);
    }

    @Test
    public void testItemSearch()
    {
        driver.findElement(By.className("search"))
              .findElement(By.className("text")).sendKeys("runite ore");
        driver.findElement(By.className("search"))
              .findElement(By.tagName("form"))
              .findElements(By.tagName("input")).get(1).click();
        String headerName = driver.findElement(By.linkText("Runite ore")).getText();

        assertEquals("Runite ore", headerName);
    }

    @Test
    public void testItemOfTheWeek()
    {
        driver.findElement(By.cssSelector("article.basic.item.member"))
              .findElement(By.tagName("h3")).click();
        String articleName = driver.findElement(By.cssSelector("div.item-description.member"))
                                   .findElement(By.tagName("h2")).getText();
        String panelName   = driver.findElement(By.cssSelector("section.geSideTab.itemWeek.member"))
                                   .findElement(By.tagName("p")).getText();

        assertEquals(articleName, panelName);
    }

    @Ignore
    @Test
    public void testSearchFiltering()
    {
        driver.findElement(By.className("search"))
              .findElement(By.className("text")).sendKeys("dragon");
        driver.findElement(By.className("search"))
              .findElement(By.tagName("form"))
              .findElements(By.tagName("input")).get(1).click();

        driver.findElement(By.cssSelector("a.filter-link")).click();

    }
}
