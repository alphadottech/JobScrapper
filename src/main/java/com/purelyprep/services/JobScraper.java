package com.purelyprep.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.*;
import com.purelyprep.pojo.Proxy;
import com.purelyprep.util.Util;
import fr.dudie.nominatim.client.JsonNominatimClient;
import fr.dudie.nominatim.client.NominatimClient;
import fr.dudie.nominatim.model.Address;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.Base64;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.stream.Collectors;

public class JobScraper {

    private static final Logger log = LoggerFactory.getLogger(JobScraper.class);
    private static final List<String> BROWSERS = List.of("Firefox", "Chrome");
    private static final String ProxiesUrl = "https://free-proxy-list.net/";
    private static final Integer MAX_RETRIES = 2;
    private static final Integer MAX_SAME_SET_COUNT = 3;
//    public static final Integer MAX_JOBS = 500;
//    public static final Integer MIN_JOBS = 400;
    public static final Integer MAX_JOBS = 3;
    public static final Integer MIN_JOBS = 1;
    public static final Integer MIN_SCORE = 70;
    public static final Integer MIN_FIT = 70;
    public static final Integer MIN_HARD = 50;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();

    public JobScraper(int maxJobs, RestTemplate restTemplate) {
        this.maxJobs = maxJobs;
        this.restTemplate = restTemplate;
        this.driver = getRandomDriver();
    }

    private final RestTemplate restTemplate;
    private WebDriver driver;
    private NominatimClient geoLocator = new JsonNominatimClient(HttpClientBuilder.create().build(), "purelyprep");
    private int maxJobs;
    private double maxScore = -1;

    public List<Proxy> getProxies() {
        List<Proxy> proxies = new ArrayList<>();

        Document doc = null;
        try {
            doc = Jsoup.connect(ProxiesUrl).get();
        } catch (IOException e) {
            log.error("Error getting proxies: ", e);
            return new ArrayList<>();
        }

        Elements rows = doc.select("tr");
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() > 1) {
                String ip = cols.get(0).text();
                String port = cols.get(1).text();
                proxies.add(new Proxy(ip, port));
            }
        }

        return proxies;

//        String url = "https://proxylist.geonode.com/api/proxy-list?protocols=http&limit=500&page=1&sort_by=lastChecked&sort_type=desc";
//        return restTemplate.getForObject(url, ProxyList.class).data;

    }

    public String getProxy() {
        try {
            List<Proxy> proxies = getProxies();
            return proxies.get(ThreadLocalRandom.current().nextInt(proxies.size())).getProxyStr();
        } catch (Exception e) {
            log.error("Error getting proxy: ", e);
            log.info("Retrying proxy...");
            Util.waitRandom();
            return getProxy();
        }
    }

//    public static WebDriver getDriver(String type, String proxy) {
//        String addr = proxy.split(":")[0];
//        Integer port = Integer.parseInt(proxy.split(":")[1]);
//        if (type.equals("Firefox")) {
//            FirefoxOptions options = new FirefoxOptions();
//            options.addArguments("--headless");
//
//            if (ThreadLocalRandom.current().nextBoolean()) {
//                options.addPreference("network.proxy.type", 1);
//                options.addPreference("network.proxy.http", addr);
//                options.addPreference("network.proxy.http_port", port);
//                options.addPreference("network.proxy.socks", addr);
//                options.addPreference("network.proxy.socks_port", port);
//                options.addPreference("network.proxy.socks_remote_dns", false);
//                options.addPreference("network.proxy.ssl", addr);
//                options.addPreference("network.proxy.ssl_port", port);
//            }
//
//            WebDriver driver = new FirefoxDriver(options);
//            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(12));
//            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(1));
//            return driver;
//        } else if (type.equals("Chrome")) {
//            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless");
//            WebDriver driver = new ChromeDriver(options);
//            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(12));
//            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(1));
//            return driver;
//        }
//        return null;
//    }
public static WebDriver getDriver(String type, String proxy) {
    String addr = proxy.split(":")[0];
    Integer port = Integer.parseInt(proxy.split(":")[1]);

    WebDriver driver = null;
    int attempts = 3;

    while (attempts > 0) {
        try {
            if (type.equals("Firefox")) {
                FirefoxOptions options = new FirefoxOptions();
                options.setBinary("/usr/bin/firefox");
                options.addArguments("--headless"); // Optional: Remove if not needed

                if (ThreadLocalRandom.current().nextBoolean()) {
                    options.addPreference("network.proxy.type", 1);
                    options.addPreference("network.proxy.http", addr);
                    options.addPreference("network.proxy.http_port", port);
                    options.addPreference("network.proxy.socks", addr);
                    options.addPreference("network.proxy.socks_port", port);
                    options.addPreference("network.proxy.socks_remote_dns", false);
                    options.addPreference("network.proxy.ssl", addr);
                    options.addPreference("network.proxy.ssl_port", port);
                }

                driver = new FirefoxDriver(options);
            } else if (type.equals("Chrome")) {
                ChromeOptions options = new ChromeOptions();
                // options.addArguments("--headless"); // Optional: Remove if not needed
                driver = new ChromeDriver(options);
            }

            if (driver != null) {
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(12));
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(1));
                break;  // Exit loop on success
            }

        } catch (SessionNotCreatedException e) {
            attempts--;
            System.out.println("Session creation failed. Retries left: " + attempts);
            if (attempts == 0) {
                System.out.println("Failed to create WebDriver session after multiple attempts.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            break;  // Exit loop on unexpected error
        }
    }

    return driver;
}
    public WebDriver getRandomDriver() {
        return getDriver(BROWSERS.get(ThreadLocalRandom.current().nextInt(BROWSERS.size())), getProxy());
    }

    public static String executeScript(WebDriver driver, String script) {
        if (driver instanceof JavascriptExecutor) {
            Object resp = ((JavascriptExecutor)driver).executeScript(script);
            if (resp != null) {
                return resp.toString();
            }
        } else {
            log.error("This driver does not support JavaScript");
        }
        return "";
    }

    public static WebElement driverFindElementByClass(SearchContext context, String className) {
        try {
            return context.findElement(By.className(className));
        } catch (Exception e) {
            return null;
        }
    }

    public static List<WebElement> driverFindElementsByClass(SearchContext context, String className) {
        try {
            return context.findElements(By.className(className));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static Element soupFindElementByClass(Element element, String className) {
        try {
            return element.getElementsByClass(className).first();
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Element> soupFindElementsByClass(Element element, String className) {
        try {
            return element.getElementsByClass(className);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String soupGetElementText(Element element, String className, String fallback) {
        Element foundElement = soupFindElementByClass(element, className);
        if (foundElement != null) {
            return foundElement.text().trim();
        }
        return fallback;
    }

    public static boolean validateLinkedInPage(WebDriver driver) {
        WebElement elem = driverFindElementByClass(driver, "authwall-join-form__title");
        return elem == null;
    }

    public static String extractResumeText(String candidateResumeBase64, String type) {
        String candidateResume = "";
        byte[] resumeBytes = Base64.getDecoder().decode(candidateResumeBase64);
        try {
            if (type.equals("pdf")) {
                PDDocument document = PDDocument.load(resumeBytes);
                PDFTextStripper pdfStripper = new PDFTextStripper();
                candidateResume = pdfStripper.getText(document);
                document.close();
            } else if (type.equals("doc") || type.equals("docx")) {
                XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resumeBytes));
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                candidateResume = extractor.getText();
                extractor.close();
                doc.close();
            } else {
                candidateResume = new String(resumeBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return candidateResume.replace("\"", "\\\"");
    }

    public static String getChatResult(List<Message> messages, int tries) {
        try {
            for (Message message : messages) {
                message.content = message.content.substring(0, Math.min(5000, message.content.length()));
            }
            String resp = OpenAiService.getInstance().chat(messages);
            if (resp == null) {
                return null;
            } else {
                return resp.replace("```json", "").replace("```", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (tries > 10) {
                return null;
            }
            Util.waitRandom(1000, 2000);
            return getChatResult(messages, tries + 1);
        }
    }

    public static List<String> getLocations(Element baseCard, String descr) {
        List<String> locations = new ArrayList<>();
        String location = soupGetElementText(baseCard, "job-search-card__location", "");
        if (location != null && !location.isEmpty()) {
            locations.add(location);
        }
        return locations.stream()
                .filter(l -> l != null && !l.trim().isEmpty())
                .map(String::trim).collect(Collectors.toList());
    }

    public static String getCandidateJobKey(String candidateId, String companyName, String title) {
        return candidateId + "/" + Job.getJobUuid(companyName, title);
    }

    public static boolean alreadySent(String candidateId, String companyName, String title) {
        return RedisService.getInstance().exists(getCandidateJobKey(candidateId, companyName, title));
    }

    public static void setSent(String candidateId, String companyName, String title) {
        RedisService.getInstance().set(getCandidateJobKey(candidateId, companyName, title), true, 15, TimeUnit.DAYS);
    }

    public static Double salaryToDouble(String salary) {
        return Double.parseDouble(salary.trim().split("/")[0].trim()
            .replace("$", "")
            .replace("€", "")
            .replace(",", ""));
    }

    private static double replyToDouble(String reply) {
        try {
            reply = reply.replaceAll("[^0-9\\.]", "");
            while (reply.endsWith(".")) {
                reply = reply.substring(0, reply.length() - 1);
            }
            return Double.parseDouble(reply);
        } catch(Exception ignored) {
            return 0;
        }
    }

    public WebDriver gotoUrlRetry(Exception e, String url, String waitForId, String waitForClass, Function<WebDriver, Boolean> customValidate, int retries) {
        Util.waitRandom(200, 2000);
        closeDriver();
        driver = getRandomDriver();
        log.info("GoTo Url Trying Again [" + retries + "]");
        return gotoUrl(url, waitForId, waitForClass, customValidate, retries);
    }

    public WebDriver gotoUrl(String url, String waitForId, String waitForClass, Function<WebDriver, Boolean> customValidate, int retries) {
        log.info("Getting: " + url);

        try {
            driver.get(url);

            if (customValidate != null && !customValidate.apply(driver)) {
                throw new Exception("Validation Failed");
            }

            if (waitForId != null) {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id(waitForId)));
            } else if (waitForClass != null) {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.className(waitForClass)));
            }
        } catch (Exception e) {
            if (retries <= MAX_RETRIES) {
                return gotoUrlRetry(e, url, waitForId, waitForClass, customValidate, retries + 1);
            } else {
                log.info("GotoUrl Retries Exhausted");
                closeDriver();
                return null;
            }
        }

        return driver;
    }

    public List<Element> getJobs(
            String jobTitleInputId,
            String jobTitle,
            boolean fullRemote,
            int retries,
            Set<String> jobSet,
            Collection<String> locations,
            boolean splitByCountry,
            int lastSetCount,
            int sameSetCount
    ) {
        try {
            String url = "https://www.linkedin.com/jobs/search?keywords=" + URLEncoder.encode(jobTitle, StandardCharsets.UTF_8);
            if (fullRemote) {
                url += "&f_WT=2";
            } else if(locations != null && !locations.isEmpty()) {
                String locs = locations.stream()
                        .map(CityToId::getId)
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                url += "&f_PP=" + URLEncoder.encode(locs, StandardCharsets.UTF_8);
                if (splitByCountry) {
                    String loc = new ArrayList<>(locations).get(0);
                    String[] parts = loc.split(",");
                    if (parts.length > 1) {
                        String country = parts[1].trim();
                        country = country.substring(0, 1).toUpperCase() + country.substring(1);
                        url += "&location=" + URLEncoder.encode(country);
                    }
                }
            }
            driver = gotoUrl(url, jobTitleInputId, null, JobScraper::validateLinkedInPage, -50);

            boolean firstRun = true;
            while (firstRun || !validateLinkedInPage(driver)) {
                if (!firstRun) {
                    log.info("Invalid LinkedIn Page. Trying again...");
                    driver = gotoUrl(url, jobTitleInputId, null, JobScraper::validateLinkedInPage, -50);
                }
                firstRun = false;
                Util.waitRandom(2000, 4000);
            }

            List<WebElement> jobList = driverFindElementsByClass(driverFindElementByClass(driver, "jobs-search__results-list"), "base-card");
            log.info("Initial Job Count: " + jobList.size());

            int tries = 0;
            int lastJobCount = 0;
            int quietCount = 0;
            while (jobList.size() < maxJobs && tries < (maxJobs + 100) && quietCount <= 10) {
                try {
                    executeScript(driver, "window.scrollTo(0, document.body.scrollHeight);");
                    Util.waitRandom(100, 250);
                    executeScript(driver, "window.scrollTo(0, document.body.scrollHeight - 750);");
                    Util.waitRandom(100, 250);
                    executeScript(driver, "window.scrollTo(0, document.body.scrollHeight - 500);");
                    Util.waitRandom(100, 250);
                    executeScript(driver, "window.scrollTo(0, document.body.scrollHeight - 250);");
                    Util.waitRandom(100, 250);
                    executeScript(driver, "window.scrollTo(0, 0);");
                    Util.waitRandom(500, 1000);
                    executeScript(driver, "window.scrollTo(0, document.body.scrollHeight);");
                    Util.waitRandom(1000, 1500);
                    WebElement scrollButton = driverFindElementByClass(driver, "infinite-scroller__show-more-button");
                    if (scrollButton != null) {
                        executeScript(driver, "document.getElementsByClassName('infinite-scroller__show-more-button')[0].click()");
                    }
                    Util.waitRandom(2000, 4000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                jobList = driverFindElementsByClass(driverFindElementByClass(driver, "jobs-search__results-list"), "base-card");
                tries++;
                int currJobCount = jobList.size();
                if (lastJobCount == currJobCount) {
                    quietCount++;
                } else {
                    quietCount = 0;
                }
                lastJobCount = currJobCount;
                log.info("Job Count So Far: " + currJobCount);
            }

            Util.waitRandom(2000, 4000);
            Document doc = Jsoup.parse(driver.getPageSource());
            List<Element> jobCards = soupFindElementsByClass(soupFindElementByClass(doc, "jobs-search__results-list"), "base-card");
            jobCards = jobCards.stream().filter(c -> !jobSet.contains(c.attr("data-entity-urn"))).collect(Collectors.toList());
            jobSet.addAll(jobCards.stream().map(c -> c.attr("data-entity-urn")).collect(Collectors.toList()));

            if (jobSet.size() < MIN_JOBS && jobCards.size() < MIN_JOBS && retries <= MAX_RETRIES && sameSetCount < MAX_SAME_SET_COUNT) {
                if (jobSet.size() == lastSetCount) {
                    sameSetCount++;
                } else {
                    sameSetCount = 0;
                }
                log.info("Job Count [" + jobCards.size() + "] (Set Count: " + jobSet.size() + ") vs [" + MIN_JOBS + "] Too Small, Trying again...");
                Util.waitRandom(3000, 6000);
                jobCards.addAll(getJobs(jobTitleInputId, jobTitle, fullRemote, retries + 1, jobSet, locations, splitByCountry, jobSet.size(), sameSetCount));
            }

            if (retries == 0) {
                log.info("Final Job Count: [" + jobCards.size() + "]");
            } else {
                log.info("Intermediate Retry Job Count: [" + jobCards.size() + "]");
            }
            return jobCards;
        } catch (Exception e) {
            log.error("Error getting jobs: ", e);
            closeDriver();
            return null;
        }
    }

    public Job processJob (
        String candidateId,
        Element baseCard,
        Map<String, Boolean> jobsDict,
        double lowestSalaryNum,
        Integer maxJobAgeDays,
        List<String> undesiredRoles,
        Set<String> undesiredCompanies,
        String resumeData,
        String resume,
       // boolean manager,
        String desiredTitle
        //int seniorityLevel
    ) {
        try {
            String jobUrn = baseCard.attr("data-entity-urn");
            String jobId = jobUrn.split(":")[3];
            if (jobsDict.containsKey(jobId)) {
                return null;
            }
            Element postedElem = soupFindElementByClass(baseCard, "job-search-card__listdate");
            if (postedElem != null) {
                LocalDate postedDate = LocalDate.parse(postedElem.attr("datetime"));
                if (postedDate.isBefore(LocalDate.now().minusDays(maxJobAgeDays))) {
                    log.info("Too old: " + postedDate);
                    return null;
                }
            }
            String title = soupGetElementText(baseCard, "base-search-card__title", "");
            log.info("Job: " + title);
            String companyName = soupGetElementText(baseCard, "base-search-card__subtitle", "");

            if (companyName.contains("Get It Recruit")) {
                log.info("Get It Recruit");
                return null;
            }

            if (undesiredCompanies != null && !undesiredCompanies.isEmpty()) {
                for (String undesiredCompany : undesiredCompanies) {
                    if (companyName.toLowerCase().contains(undesiredCompany.toLowerCase())) {
                        log.info("Undesired company: [" + undesiredCompany + "]");
                        return null;
                    }
                }
            }

            if (title.toLowerCase().contains("contract")) {
                log.info("Contract: [" + title + "]");
                return null;
            }

            if (alreadySent(candidateId, companyName, title)) {
                log.info("Already sent");
                return null;
            }

            String jobDetailsUrl = "https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/" + jobId;
            JobDetails jobDetails = processJobDetails(jobDetailsUrl, 0);

            if (jobDetails == null) {
                log.info("Null Job Details");
                return null;
            }

            if (!jobDetails.salary.isEmpty()) {
                String[] salaryRng = jobDetails.salary.split(" - ");
                double minSalary = salaryToDouble(salaryRng[0]);
                double maxSalary = (salaryRng.length > 1) ? salaryToDouble(salaryRng[1]) : minSalary;
                if (maxSalary < lowestSalaryNum) {
                    log.info("Salary too low: [" + jobDetails.salary + "]");
                    return null;
                }
            }

            if (jobDetails.jobDescr.contains("Get It Recruit")) {
                log.info("Get It Recruit");
                return null;
            }

            if (jobDetails.jobDescr.contains("volunteer")) {
                log.info("Volunteer");
                return null;
            }

            if (jobDetails.itemDict.containsKey("employment type") && jobDetails.itemDict.get("employment type").contains("contract")) {
                log.info("Contract Role");
                return null;
            }

            /*String jobSeniorityLevel = "";
            if (jobDetails.itemDict.containsKey("seniority level")) {
                jobSeniorityLevel = jobDetails.itemDict.get("seniority level");
                if (seniorityLevel > -1 && Job.seniorityLevelToInt(jobSeniorityLevel) > seniorityLevel) {
                    log.info("Mismatched Seniority: [" + jobSeniorityLevel + "]");
                    return null;
                }
            }
*/
            String jobRequirements = "";

            if (undesiredRoles != null && !undesiredRoles.isEmpty()) {
                jobRequirements = getJobDescrData(jobDetails.jobDescr);
                if (!passesUndesiredList(undesiredRoles, title, jobRequirements)) {
                    log.info("Undesired Role");
                    return null;
                }
            }

            List<String> locations = getLocations(baseCard, jobDetails.jobDescr);

            if (jobRequirements.isEmpty()) {
                jobRequirements = getJobDescrData(jobDetails.jobDescr);
            }

            String hardRequirements = getHardRequirements(jobDetails.jobDescr);
            double hardReqMatch = getHardRequirementsMatch(hardRequirements, resume);

            if (hardReqMatch < MIN_HARD) {
                log.info("Hard Requirements Mismatch");
                return null;
            }

            double score = getSkillsRequirementsMatchScore(resumeData, jobRequirements);
            if (score < MIN_SCORE) {
                log.info("Too low first layer score: [" + score + "]");
                return null;
            }

            //if (manager && !jobHasManagementFacet(title, jobRequirements)) {
            if (!jobHasManagementFacet(title, jobRequirements)) {
                log.info("No Management Facet");
                return null;
            }


            double fitScore = getFitScore(desiredTitle, resumeData, title, jobRequirements);
            if (fitScore < MIN_FIT) {
                log.info("Too low fit score: [" + fitScore + "]");
                return null;
            }

            if (jobDetails.salary == null || jobDetails.salary.isEmpty()) {
                double maxSalary = getSalary(jobDetails.jobDescr);
                if (maxSalary > 0) {
                    if (maxSalary < lowestSalaryNum) {
                        log.info("Extracted salary too low: [" + maxSalary + "]");
                        return null;
                    }
                    jobDetails.salary = currencyFormatter.format(maxSalary);
                }
            }

            score = (0.5 * score) + (0.3 * fitScore) + (0.2 * hardReqMatch);
            if (score > maxScore) {
                maxScore = score;
            }
            log.info("Score: " + maxScore + ", Max: " + maxScore);

            Job job = new Job(
                jobId,
                title,
                companyName,
                jobDetailsUrl,
                jobDetails.salary,
                jobDetails.jobDescr,
                Math.ceil(score),
                getExplanation(desiredTitle, resumeData, title, jobRequirements),
                locations,
                //jobSeniorityLevel,
                Util.getNow()
            );

            jobsDict.put(jobId, true);
            return job;
        } catch (Exception e) {
            log.error("Error processing job: ", e);
            return null;
        }
    }

    public JobDetails processJobDetails(String jobDetailsUrl, int retries) {
        driver = gotoUrl(jobDetailsUrl, null, "description__job-criteria-list", JobScraper::validateLinkedInPage, 0);
        if (driver == null) {
            driver = getRandomDriver();
            return null;
        }

        String source = driver.getPageSource();
        Document soup = Jsoup.parse(source);

        String salary = soupGetElementText(soup, "compensation__salary", "");

        String jobDescr = soupGetElementText(soup, "show-more-less-html__markup", "")
                .trim()
                .replace("\\n", "\\\\n");
        jobDescr = jobDescr.replaceAll("\\.([A-Z])", ". $1").replace("\"", "\\\"");

        if (retries < 3 && salary.isEmpty() && !jobDescr.isEmpty()) {
            log.info("Salary not found, retrying...");
            Util.waitRandom(1000, 4000);
            return processJobDetails(jobDetailsUrl, retries + 1);
        }

        Map<String, String> itemDict = new HashMap<>();
        List<Element> items = soupFindElementsByClass(soup, "description__job-criteria-item");
        for (Element item : items) {
            String key = soupGetElementText(item, "description__job-criteria-subheader", "")
                    .toLowerCase().trim();
            String value = soupGetElementText(item, "description__job-criteria-text", "")
                    .toLowerCase().trim();
            itemDict.put(key, value);
        }

        return new JobDetails(salary, jobDescr, itemDict);
    }

    private String getResumeData(String resume) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.user, "Extract the Candidate Resume Skills and Candidate Work Experience " +
                "from this resume:\n" +
                "\"\"\"\n" +
                resume + "\n" +
                "\"\"\"\n" +
                "\n" +
                "Format the output as:\n" +
                "- Candidate Resume Skills: [List]\n" +
                "- Candidate Work Experience: [List]"));
        return getChatResult(messages, 0);
    }

    private String getJobDescrData(String jobDescr) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.user, "Extract the Primary Skills and Required Work Experience (also called Qualifications) from this " +
                "job description:\n" +
                "\"\"\"\n" +
                jobDescr + "\n" +
                "\"\"\"\n" +
                "\n" +
                "Format the output as:\n" +
                "- Primary Skills: [List]\n" +
                "- Required Work Experience: [List]"));
        return getChatResult(messages, 0);
    }

    private String getHardRequirements(String jobDescr) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.user, "**Task: Extracting Hard Requirements from a Job Description**\n" +
                "\n" +
                "**Objective:**\n" +
                "You are tasked with identifying and extracting the essential qualifications and hard requirements from a given job description. " +
                "These hard requirements are the minimum qualifications that every candidate must possess to be considered for the position. " +
                "The extracted information will assist an automated hiring system in quickly and efficiently filtering through resumes.\n" +
                "\n" +
                "**Instructions:**\n" +
                "\n" +
                "1. **Read the Job Description Carefully:**\n" +
                "   - Look for sections typically labeled as \"Requirements\", \"Qualifications\", or \"Must-Have Skills\".\n" +
                "   - Identify any statements that indicate non-negotiable qualifications, such as \"must have\", \"required\", or " +
                        "\"mandatory\".\n" + // Treat \"preferred\" as mandatory.\n" +
                "\n" +
                "2. **Extract Hard Requirements:**\n" +
                "   - Extract qualifications related to experience, technical skills, education, certifications, and any " +
                "other must-have criteria.\n" +
                "   - Ignore \"soft skills\" or any non-essential qualifications.\n" +
                "\n" +
                "3. **Present the Requirements as a List:**\n" +
                "   - Format the extracted requirements in a clear, concise list.\n" +
                "   - Ensure that each requirement is on a separate line for easy readability and processing by automated systems."));
        messages.add(new Message(Role.user, "Here is the job description: " + jobDescr));
        return getChatResult(messages, 0);
    }

    private double getHardRequirementsMatch(String hardRequirements, String resume) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.user, "**Task: Calculating Percentage of Hard Requirements Met by a Given Resume**\n" +
                "\n" +
                "**Objective:**\n" +
                "Compare a provided resume against a predefined list of hard requirements and calculate the percentage of requirements " +
                "that the candidate meets.\n" +
                "\n" +
                "**Instructions:**\n" +
                "\n" +
                "1. **Input Data:**\n" +
                "   - You will be given a list of hard requirements.\n" +
                "   - You will also be given a resume to review.\n" +
                "\n" +
                "2. **Comparison Process:**\n" +
                "   - Review the provided resume thoroughly.\n" +
                "   - Match the qualifications on the resume against the predefined list of hard requirements.\n" +
                "   - Count the number of hard requirements met by the candidate.\n" +
                "\n" +
                "3. **Calculate and Output the Percentage:**\n" +
                "   - Calculate the percentage of hard requirements that the candidate meets.\n" +
                "   - Output only the percentage, with no additional text or explanation.\n"));
        messages.add(new Message(Role.user, "Here is the list of Hard Requirements: " + hardRequirements));
        messages.add(new Message(Role.user, "Here is the Resume: " + resume));
        messages.add(new Message(Role.assistant, "Output the final result as a percentage. Only output the " +
                "final percentage without any other text."));
        String reply = getChatResult(messages, 0);
        log.info("Hard requirement score: [" + reply + "]");
        return replyToDouble(reply);
    }

    private double getSkillsRequirementsMatchScore(String extractedResumeData, String extractedJobDescrData) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.user, "You are a hiring manager at a company. You are evaluating " +
                "many candidates for a job and need to find the best person for the job. You have a list of " +
                "required Primary Skills and Required Work Experience for the job. And you have a list of " +
                "Candidate Resume Skills and " +
                "Candidate Work Experience from a candidate's resume."));
        messages.add(new Message(Role.user, "The following are the Primary Skills and Required Work Experience " +
                "for the job: " + extractedJobDescrData));
        messages.add(new Message(Role.user, "The following are the Candidate Resume Skills and " +
                "Candidate Work Experience from the candidate's resume: \n" + extractedResumeData));
        messages.add(new Message(Role.user, "Given the Required Work Experience for the job " +
                "and the Candidate Work Experience from the candidate's resume that are listed above, evaluate the match:\n" +
                "\n" +
                "Required Work Experience: [Insert Required Work Experience from above]\n" +
                "\n" +
                "Candidate Work Experience: [Insert Candidate Work Experience from above]\n" +
                "\n" +
                "Rate the match on a scale from 1 (no match) to 10 (perfect match) for work experience. " +
                "Output the following rating: Work Experience Match (1-10)."));
        messages.add(new Message(Role.user, "Now, given the Primary Skills for the job and Candidate Resume " +
                "Skills that are listed above, evaluate the match for each skill:\n" +
                "\n" +
                "Primary Skills: [Insert Primary Skills from above]\n" +
                "\n" +
                "Candidate Resume Skills: [Insert Candidate Skills from above]\n" +
                "\n" +
                "Rate the match on a scale from 1 (no match) to 10 (perfect match) for each primary skill. " +
                "Output the following rating: Primary Skills Match (1-10)."));
        messages.add(new Message(Role.user, "Based on the match ratings calculated above for Primary Skills Match " +
                "and Work Experience Match, compute an overall rating from 0 to 100. The formula " +
                "for the overall rating is: (Primary Skills Match * 0.30) + (Work Experience Match * 0.70).\n" +
                "\n" +
                "Weight the categories as follows:\n" +
                "- Primary Skills Match: 30%\n" +
                "- Work Experience Match: 70%\n" +
                "\n" +
                "Compute the weighted sum to determine the final match score using the data given above. " +
                "Only output the final score as a percentage."));
        messages.add(new Message(Role.assistant, "Output the final result as a percentage. Only output the " +
                "final percentage without any other text."));
        String reply = getChatResult(messages, 0);
        log.info("First layer score: [" + reply + "]");

        return replyToDouble(reply);
    }

    private boolean passesUndesiredList(List<String> undesiredRoles, String jobTitle, String jobRequirements) {
        List<Message> messages = new ArrayList<>();
        String roleText = "You are a high performing, highly paid employee who is unhappy with their current job. You " +
                "are looking for a new job, but you can afford to be picky. You will absolutely not accept any job " +
                "with any of the following undesired components: " + String.join(", ", undesiredRoles) + ". Read " +
                "the following job title and job requirements. Slow down and take a breath. Really think about this. You " +
                "do not want to take a job with an undesired component. " +
                "Output true if the job is acceptable. " +
                "Output false if the job is unacceptable because it has an undesired component.";
        Message message = new Message(Role.system, roleText);
        messages.add(message);
        messages.add(new Message(Role.user, "The job title is: " + jobTitle));
        messages.add(new Message(Role.user, "The job requirements are: " + jobRequirements));
        messages.add(new Message(Role.assistant, "Only output true or false."));
        String reply = getChatResult(messages, 0);
        reply = (reply == null ? "false" : reply).toLowerCase()
                .replace("*", "");
        try {
            return objectMapper.readValue(reply, Boolean.class);
        } catch (Exception e) {
            log.error("Error parsing expected boolean: [" + reply + "]", e);
            return false;
        }
    }

    private boolean jobHasManagementFacet(String jobTitle, String jobRequirements) {
        List<Message> messages = new ArrayList<>();
        String roleText = "You are a high performing, highly paid employee who is unhappy with their current job. You " +
                "are looking for a new job, but you can afford to be picky. You will only accept jobs " +
                "with a managerial component. Read the following job title and job requirements. Output true if " +
                "the job is acceptable. Output false if the job is unacceptable because it has no managerial component.";
        Message message = new Message(Role.system, roleText);
        messages.add(message);
        messages.add(new Message(Role.user, "The job title is: " + jobTitle));
        messages.add(new Message(Role.user, "The job requirements are: " + jobRequirements));
        messages.add(new Message(Role.assistant, "Only output true or false."));
        String reply = getChatResult(messages, 0);
        reply = (reply == null ? "false" : reply).toLowerCase()
                .replace("*", "");
        try {
            return objectMapper.readValue(reply, Boolean.class);
        } catch (Exception e) {
            log.error("Error parsing expected boolean: [" + reply + "]", e);
            return false;
        }
    }

    private double getFitScore(String desiredTitle, String resumeSkills, String jobTitle, String jobRequirements) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.system, "You are a hiring manager. You need to make sure " +
                "a candidate's desired title and qualifications fit a given job title and job requirements. Output a " +
                "score between 0 and 100. If the desired title and given job title do not reasonably match, output a 0. " +
                "For example, someone who wants to be a Communications Manager is not qualified at all to be an " +
                "Engineering Manager. Similarly, if someone wants to be a VP of Operations, they would not be a fit " +
                "for sales, HR, or engineering roles. Your job to is output a score between 0 and 100 of how well a " +
                "given candidate's desired title and resume qualifications fits with a given job title and job requirements."));
        messages.add(new Message(Role.user, "The candidate's desired job title is: " + desiredTitle));
        messages.add(new Message(Role.user, "The candidate's resume qualifications are: " + resumeSkills));
        messages.add(new Message(Role.user, "The given job title is: " + jobTitle));
        messages.add(new Message(Role.user, "The given job requirements are: " + jobRequirements));
        messages.add((new Message(Role.assistant, "Only output an integer between 0 and 100.")));
        String reply = getChatResult(messages, 0);
        log.info("Fit score: [" + reply + "]");
        return replyToDouble(reply);
    }

    private String getExplanation(String desiredTitle, String resumeSkills, String jobTitle, String jobRequirements) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.system, "You are a hiring manager who has found a really great candidate, " +
                "but you need to provide a brief, valid, and reasonable explanation to the hiring team in order to " +
                "convince them to hire this person. You will be given the candidate's desired title, the candidate's " +
                "resume qualifications, the actual job title, and the actual job requirements. Be specific about why " +
                "this candidate is a fit and justify any gaps in experience they might have."));
        messages.add(new Message(Role.user, "The candidate's desired job title is: " + desiredTitle));
        messages.add(new Message(Role.user, "The candidate's resume qualifications are: " + resumeSkills));
        messages.add(new Message(Role.user, "The actual job title is: " + jobTitle));
        messages.add(new Message(Role.user, "The actual job requirements are: " + jobRequirements));
        messages.add(new Message(Role.assistant, "Only output your brief explanation. Do not restate the question in any way."));
        String explanation = getChatResult(messages, 0);
        log.info("Explanation: [" + explanation + "]");
        return explanation;
    }

    private double getSalary(String jobDescr) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(Role.system, "Find the highest salary or base pay in the following job description. Output the " +
                "salary (or base pay) as an integer. For example, if you see text like this: \"up to $107,200/year\", then output 107200. " +
                "If no salary or base pay can be found, output a 0."));
        messages.add(new Message(Role.user, "The job description is: " + jobDescr));
        messages.add(new Message(Role.assistant, "Only output the salary or base pay as an integer. If you cannot find one, output a 0"));
        String salary = getChatResult(messages, 0);
        return replyToDouble(salary);
    }

    private LatLong getLatLong(String loc) {
        List<Address> results;
        try {
            results = geoLocator.search(loc);
            if (results != null && results.size() > 0) {
                return new LatLong(results.get(0).getLatitude(), results.get(0).getLongitude());
            }
        } catch (IOException e) {
            log.error("Error getting lat/long: ", e);
        }
        return null;
    }

    private boolean isLocationClose(LatLong desiredLatLng, String loc, double distThresholdMiles) {
        LatLong locLatLng = getLatLong(loc);
        if (locLatLng != null) {
            return Util.distance(desiredLatLng, locLatLng) < distThresholdMiles;
        }
        return false;
    }

    public JobResult getJobsForCandidate(CandidatePreferences prefs, boolean setSent) {
        JobResult resp = new JobResult();

        if (prefs.candidateResumePath != null && !prefs.candidateResumePath.isEmpty()) {
            prefs.candidateResume = extractResumeText(
                Util.encodeFileToBase64(prefs.candidateResumePath),
                prefs.candidateResumePath.split("\\.")[1]
            );
            prefs.candidateResumePath = "";
        }

        if (prefs.fullRemote && prefs.desiredLocations != null && !prefs.desiredLocations.isEmpty()) {
            CandidatePreferences remoteClone = prefs.clone();
            remoteClone.desiredLocations = new ArrayList<>();
            log.info("Getting remote jobs first");
            resp.merge(getJobsForCandidate(remoteClone, false));
            prefs.fullRemote = false;
        }

        if (prefs.splitByCountry && prefs.desiredLocations != null && prefs.desiredLocations.size() > 1) {
            for (String loc : prefs.desiredLocations) {
                CandidatePreferences remoteClone = prefs.clone();
                remoteClone.desiredLocations = Collections.singletonList(loc.trim());
                log.info("Getting jobs from: [" + loc + "]");
                resp.merge(getJobsForCandidate(remoteClone, false));
            }
        } else {
            for (String jobTitle : prefs.jobTitles) {
                resp.merge(getJobsForCandidate(
                    prefs.candidateId,
                    jobTitle,
                   // prefs.manager,
                    prefs.desiredLocations,
                    prefs.fullRemote,
                    prefs.lowestSalary,
                    prefs.undesiredRoles,
                    prefs.undesiredCompanies,
                    prefs.candidateResume,
                    prefs.maxJobAgeDays,
                    setSent,
                    prefs.splitByCountry
                    //Job.seniorityLevelToInt(prefs.seniorityLevel)
                ));
            }
        }

        if (setSent) {
            ScheduleService.getInstance().setLastSent(prefs.candidateId);
        }

        return resp;
    }

    private JobResult getJobsForCandidate(
        String candidateId,
        String jobTitle,
        //boolean manager,
        List<String> desiredLocations,
        boolean fullRemote,
        String lowestSalary,
        List<String> undesiredRoles,
        Set<String> undesiredCompanies,
        String candidateResume,
        Integer maxJobAgeDays,
        boolean setSent,
        boolean splitByCountry
        //int seniorityLevel
    ) {
        double lowestSalaryNum = Double.parseDouble(lowestSalary.replace("$", "").replace(",", ""));
        Map<String, Boolean> jobsDict = new HashMap<>();
        List<Job> jobsArr = new ArrayList<>();

        List<Element> jobList = null;
        try {
            while (jobList == null) {
                jobList = getJobs("job-search-bar-keywords", jobTitle, fullRemote, 0, new HashSet<>(), desiredLocations, splitByCountry, 0, 0);
            }

            String resumeData = getResumeData(candidateResume);

            for (Element baseCard : jobList) {
                Job job = processJob(
                    candidateId,
                    baseCard,
                    jobsDict,
                    lowestSalaryNum,
                    maxJobAgeDays,
                    undesiredRoles,
                    undesiredCompanies,
                    resumeData,
                    candidateResume,
                   // manager,
                    jobTitle
                    //seniorityLevel
                );

                if (job != null) {
                    jobsDict.put(job.jobId, true);
                    if (setSent) {
                        setSent(candidateId, job.companyName, job.title);
                    }
                    jobsArr.add(job);
                }
            }

            closeDriver();
        } catch (Exception e) {
            closeDriver();
            log.error("Error getting and processing jobs: ", e);
        }

        log.info(
            "All Done! " +
            (jobList != null ? "Processed [" + jobList.size() + "] Jobs" : "No Jobs Processed") +
            " with [" + jobsArr.size() + "] Jobs Sent"
        );

        return new JobResult(jobsArr, jobList.size(), Collections.singleton(jobTitle));
    }

    private void closeDriver() {
        try {
            driver.quit();
        } catch (Exception ignored) {
        }
    }

}
