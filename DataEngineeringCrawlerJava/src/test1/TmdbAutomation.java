package test1;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TmdbAutomation {

    public static void main(String[] args) throws Exception {

    	ChromeOptions options = new ChromeOptions();
        options.addArguments("--blink-settings=imagesEnabled=false"); // Disable images
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        // 2. SETUP CSV WRITER
        PrintWriter csvWriter = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream("MovieData_1100.csv"), StandardCharsets.UTF_8)
        );
        csvWriter.println("Movie Name,User Score,Release Year,Genre,Budget,Revenue,Actors,Director");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        int recordsCollected = 0;
        int pageNumber = 1;
        int targetRecords = 1100;

        System.out.println("Hello World!!!!");

        // 3. MAIN LOOP (Goes through pages)
        while (recordsCollected < targetRecords) {
            
            String listUrl = "https://www.themoviedb.org/movie?page=" + pageNumber;
            driver.get(listUrl);

            // Handle Cookies (Only once on the first page)
            if (pageNumber == 1) {
                try {
                    WebElement cookieBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
                    cookieBtn.click();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    // Ignore if cookie button is not found
                }
            }

            // Wait for cards to be visible
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.card")));

            // Count how many movies are on this page (usually 20)
            int moviesOnPage = driver.findElements(By.cssSelector("div.card h2 a")).size();
            System.out.println("Page " + pageNumber + " loaded. Found " + moviesOnPage + " movies.");

            // 4. INNER LOOP (Click -> Scrape -> Back)
            for (int i = 0; i < moviesOnPage; i++) {
                
                if (recordsCollected >= targetRecords) break;

                // CRITICAL STEP: Re-find the list of movies every time.
                // If we don't do this, we get "StaleElementReferenceException" after going back.
                List<WebElement> movieCards = driver.findElements(By.cssSelector("div.card h2 a"));
                
                // Scroll slightly to make sure the element is clickable
                WebElement movieLink = movieCards.get(i);
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", movieLink);
                Thread.sleep(500); // Tiny pause for scroll

                // CLICK
                movieLink.click();

                // --- SCRAPE DATA (Same Logic as before) ---
                String name = getSimpleText(driver, "h2 a");
                String year = getSimpleText(driver, "span.release_date").replaceAll("[^0-9]", "");
                String genre = getSimpleText(driver, "span.genres");

                // User Score (Attribute)
                String userScore = "N/A";
                List<WebElement> scoreEl = driver.findElements(By.cssSelector("div.user_score_chart"));
                if (!scoreEl.isEmpty()) {
                    userScore = scoreEl.get(0).getAttribute("data-percent");
                }

                // Facts (Budget/Revenue)
                List<WebElement> facts = driver.findElements(By.cssSelector("section.facts p"));
                String budget = facts.stream().filter(e -> e.getText().contains("Budget"))
                        .findFirst().map(e -> e.getText().replace("Budget", "").trim()).orElse("N/A");
                String revenue = facts.stream().filter(e -> e.getText().contains("Revenue"))
                        .findFirst().map(e -> e.getText().replace("Revenue", "").trim()).orElse("N/A");

                // Director
                String director = driver.findElements(By.cssSelector("li.profile"))
                        .stream().filter(e -> e.getText().contains("Director"))
                        .findFirst().map(e -> e.findElement(By.cssSelector("a")).getText()).orElse("N/A");

                // Actors
                String actors = driver.findElements(By.cssSelector("ol.people li.card p a"))
                        .stream().limit(5).map(WebElement::getText).collect(Collectors.joining(", "));

                // Save
                String csvLine = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        name, userScore, year, genre, budget, revenue, actors, director);
                csvWriter.println(csvLine);
                csvWriter.flush();

                System.out.println("[" + (recordsCollected + 1) + "] " + name);
                recordsCollected++;

                // --- NAVIGATE BACK & WAIT ---
                driver.navigate().back();
                
                // Wait to prevent blocking (3-5 seconds as requested)
                System.out.println("Waiting 4 seconds...");
                Thread.sleep(4000); 
                
                // Wait for the list to be ready again before next loop iteration
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.card")));
            }

            pageNumber++;
        }

        csvWriter.close();
        driver.quit();
        System.out.println("DONE! Saved 1100 records.");
    }

    // Helper
    public static String getSimpleText(WebDriver driver, String selector) {
        List<WebElement> els = driver.findElements(By.cssSelector(selector));
        if (els.isEmpty()) return "N/A";
        return els.get(0).getText().trim();
        }
    }

