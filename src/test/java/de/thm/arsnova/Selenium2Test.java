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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import de.thm.arsnova.dao.CouchDBDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.services.FeedbackService;
import de.thm.arsnova.services.SessionService;
import de.thm.arsnova.services.StubUserService;

public class Selenium2Test {

	private ARSnovaChromeDriver driver;
	private Properties properties;

	private CouchDBDao couchdbDao;
	private SessionService sessionService;
	private StubUserService userService;
	private FeedbackService feedbackService;

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
		feedbackService = new FeedbackService();
		feedbackService.setDatabaseDao(couchdbDao);

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
	public final void studentGuestShouldPostFeedback() {
		Session session = couchdbDao.saveSession(createSession());
		Feedback initialFeedback = feedbackService.getFeedback(session.getKeyword());

		selectStudentRole();
		loginAsGuest();
		joinSession(session);

		WebElement feedbackBadButton = waitForElement(By.className("feedbackBad"));
		// Before clicking, ensure that no loading mask is displayed
		By loadingSpinner = By.className("x-mask");
		waitWhileVisible(loadingSpinner);
		feedbackBadButton.click();
		
		// Wait for the feedback to arrive back at the client
		By feedbackResultToolbar = By.id("ext-comp-1125");
		waitForElementWithContent(feedbackResultToolbar, "1/");
		
		Feedback feedback = feedbackService.getFeedback(session.getKeyword());
		assertEquals(new Feedback(0, 0, 0, 0), initialFeedback);
		assertEquals(new Feedback(0, 0, 1, 0), feedback);
	}

	private void waitForElementWithContent(final By by, final String content) {
		final long timeoutInSecs = 10;
		(new WebDriverWait(driver, timeoutInSecs)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(final WebDriver d) {
				WebElement element = d.findElement(by);
				return element != null && element.getText().contains(content);
			}
		});
	}

	private void assertEquals(Feedback feedback, Feedback feedback2) {
		assertTrue(feedback.equals(feedback2));
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
	
	private void waitWhileVisible(final By by) {
		final long timeoutInSecs = 10;
		(new WebDriverWait(driver, timeoutInSecs)).until(ExpectedConditions.invisibilityOfElementLocated(by));
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

	private void joinSession(Session session) {
		WebElement sessionKeywordField = waitForElement(By.name("keyword"));
		sessionKeywordField.sendKeys(session.getKeyword());
		WebElement joinSessionButton = waitForElement(By.id("ext-gen1138"));
		joinSessionButton.click();
	}

	private void loginAsGuest() {
		WebElement guestLoginButton = waitForElement(By.id("ext-gen1016"));
		guestLoginButton.click();
	}

	private void selectStudentRole() {
		WebElement studentRoleButton = waitForElement(By.id("ext-gen1047"));
		studentRoleButton.click();
	}
}
