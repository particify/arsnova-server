package de.thm.arsnova;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.support.ui.*;
import org.springframework.core.io.*;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import de.thm.arsnova.dao.CouchDBDao;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.services.*;

public class Selenium2Test {

	ARSnovaChromeDriver driver;
	Properties properties;

	CouchDBDao couchdbDao;
	SessionService sessionService;
	StubUserService userService;

	@Before
	public void setUp() throws IOException {
		Resource resource = new FileSystemResource("/etc/arsnova/arsnova.properties");
		properties = PropertiesLoaderUtils.loadProperties(resource);

		userService = new StubUserService();
		userService.setUserAuthenticated(true);

		couchdbDao = new CouchDBDao();
		couchdbDao.setDatabaseHost(properties.getProperty("couchdb.host", "localhost"));
		couchdbDao.setDatabasePort(properties.getProperty("couchdb.port", "5984"));
		couchdbDao.setDatabaseName(properties.getProperty("couchdb.name", "arsnova"));
		sessionService = new SessionService();
		couchdbDao.setSessionService(sessionService);
		couchdbDao.setUserService(userService);
		sessionService.setDatabaseDao(couchdbDao);

		this.driver = new ARSnovaChromeDriver();
		driver.get(properties.getProperty("security.arsnova-url", "http://localhost:8080/arsnova-war/"));
		LocalStorage localStorage = this.driver.getLocalStorage();
		localStorage.setItem("html5 info read", "");
	}

	@After
	public void tearDown() {
		driver.close();
		driver.quit();
	}

	@Test
	public void studentGuestShouldBeAbleToJoinSession() {
		Session session = new Session();
		session.setName("selenium test session");
		session.setShortName("selenium");
		session = couchdbDao.saveSession(session);

		WebElement studentRoleButton = waitForElement(By.id("ext-gen1047"));
		studentRoleButton.click();

		WebElement guestLoginButton = waitForElement(By.id("ext-gen1016"));
		guestLoginButton.click();

		WebElement sessionKeywordField = waitForElement(By.name("keyword"));
		sessionKeywordField.sendKeys(session.getKeyword());
		WebElement joinSessionButton = waitForElement(By.id("ext-gen1138"));
		joinSessionButton.click();

		WebElement feedbackGoodButton = waitForElement(By.className("feedbackGood"));
		assertTrue(feedbackGoodButton.isDisplayed());
	}

	private WebElement waitForElement(final By by) {
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(final WebDriver d) {
				return d.findElement(by) != null;
			}
		});
		return driver.findElement(by);
	}
}