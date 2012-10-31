package de.thm.arsnova;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import de.thm.arsnova.dao.CouchDBDao;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.services.SessionService;
import de.thm.arsnova.services.StubUserService;

public class Selenium2Test {

	private ARSnovaChromeDriver driver;
	private Properties properties;

	private CouchDBDao couchdbDao;
	private SessionService sessionService;
	private StubUserService userService;

	@Before
	public final void setUp() throws IOException {
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
	public final void tearDown() {
		driver.close();
		driver.quit();
	}

	@Test
	public final void studentGuestShouldSeeFeedbackButtonsAfterJoiningSession() {
		Session session = couchdbDao.saveSession(createSession());

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
		final long timeoutInSecs = 10;
		(new WebDriverWait(driver, timeoutInSecs)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(final WebDriver d) {
				return d.findElement(by) != null;
			}
		});
		return driver.findElement(by);
	}

	private Session createSession() {
		return createNamedSession(null, null);
	}

	private Session createNamedSession(final String name, final String shortName) {
		Session session = new Session();
		session.setName(name != null ? name : "selenium test session");
		session.setShortName(shortName != null ? shortName : "selenium");
		return session;
	}
}