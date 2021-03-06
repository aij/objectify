/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.GAETestBase;
import com.googlecode.objectify.test.util.TestObjectifyFactory;
import com.googlecode.objectify.test.util.TestObjectifyService;
import com.googlecode.objectify.util.Closeable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests of defer()
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeferTests extends GAETestBase
{
	@BeforeMethod
	public void setUp() throws Exception {
		TestObjectifyService.setFactory(new TestObjectifyFactory());

		fact().register(Trivial.class);
	}

	/** */
	@Test
	public void deferredSaveAndDeleteProcessedAtEndOfRequest() throws Exception {

		Trivial triv = new Trivial(123L, "foo", 5);

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().defer().save().entity(triv);

			// Can load out of session
			assertThat(ofy().load().entity(triv).now(), is(triv));

			// But not the datastore
			try {
				ds().get(null, Key.create(triv).getRaw());
				assert false : "Entity should not have been saved yet";
			} catch (EntityNotFoundException e) {
				// correct
			}
		}

		try (Closeable root = TestObjectifyService.begin()) {
			Trivial loaded = ofy().load().entity(triv).now();
			assertThat(loaded, equalTo(triv));
		}

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().defer().delete().entity(triv);

			// Deleted in session
			assertThat(ofy().load().entity(triv).now(), nullValue());

			// But not datastore
			try {
				ds().get(null, Key.create(triv).getRaw());
			} catch (EntityNotFoundException e) {
				assert false : "Entity should not have been deleted yet";
			}
		}

		try (Closeable root = TestObjectifyService.begin()) {
			Trivial loaded = ofy().load().entity(triv).now();
			assertThat(loaded, nullValue());
		}
	}

	/** */
	@Test
	public void deferredSaveAndDeleteProcessedAtEndOfTransaction() throws Exception {

		final Trivial triv = new Trivial(123L, "foo", 5);

		try (Closeable root = TestObjectifyService.begin()) {

			ofy().transact(new VoidWork() {
				@Override
				public void vrun() {
					ofy().defer().save().entity(triv);

					// Can load out of session
					assertThat(ofy().load().entity(triv).now(), is(triv));

					// But not datastore
					try {
						ds().get(null, Key.create(triv).getRaw());
						assert false : "Entity should not have been saved yet";
					} catch (EntityNotFoundException e) {
						// correct
					}
				}
			});

			{
				Trivial loaded = ofy().load().entity(triv).now();
				assertThat(loaded, equalTo(triv));
			}

			ofy().transact(new VoidWork() {
				@Override
				public void vrun() {
					ofy().defer().delete().entity(triv);

					// Deleted in session
					assertThat(ofy().load().entity(triv).now(), nullValue());

					// But not datastore
					try {
						ds().get(null, Key.create(triv).getRaw());
					} catch (EntityNotFoundException e) {
						assert false : "Entity should not have been deleted yet";
					}
				}
			});

			{
				Trivial loaded = ofy().load().entity(triv).now();
				assertThat(loaded, nullValue());
			}
		}
	}
}