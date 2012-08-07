/*
 * JBoss, a division of Red Hat
 * Copyright 2012, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.gatein.jcr.jta;

import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.idm.UserImpl;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;
import org.picocontainer.Startable;

import java.io.InputStream;
import java.util.logging.Level;

/**
 * Simple component for testing JBC behaviour in JTA/non-JTA environment
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Managed
@ManagedDescription("JBossCacheTransactionTest")
@NameTemplate({
      @Property(key = "name", value = "JBossCacheTransactionTest"),
      @Property(key = "service", value = "JBossCacheTransactionTest")
})
public class JBossCacheTransactionTest implements Startable
{

   private static final Logger log = LoggerFactory.getLogger(JBossCacheTransactionTest.class);

   private Cache cache;

   public static final String CONFIG_FILE_LOCATION = "conf/portal/jboss-cache.xml";

   public static final String NODE_GTN_GROUP_ID = "NODE_GTN_GROUP_ID";

   public static final String NODE_PLIDM_ROOT_GROUP = "NODE_PLIDM_ROOT_GROUP";

   public static final String NULL_NS_NODE = "GTN_IC_COMMON_NS";

   public static final String USER_QUERY_NODE = "GTN_USER_QUERY_LAZY_LIST";

   public static final String MAIN_ROOT = "NODE_GTN_ORG_SERVICE_INT_CACHE_MAIN_ROOT";

   public static final String NODE_OBJECT_KEY = "object";

   public static final int expiration = 50000;

   private OrganizationService orgService;

   public JBossCacheTransactionTest(OrganizationService orgService)
   {
      this.orgService = orgService;
   }

   private void putGtnUserLazyPageList(String ns, Object objectToPut)
   {
      Fqn nodeFqn = getFqn(ns, USER_QUERY_NODE, "null::null::null::null::null::null:::");

      Node ioNode = cache.getRoot().addChild(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, objectToPut);
         setExpiration(ioNode);

         if (log.isTraceEnabled())
         {

            log.trace(this.toString() + "GateIn user query list cached. Query: " + "null::null::null::null::null::null:::" + ";namespace=" + ns);
         }
      }
   }


   private Object getGtnUserLazyPageList(String ns)
   {

      Fqn nodeFqn = getFqn(ns, USER_QUERY_NODE, "null::null::null::null::null::null:::");

      Node node = cache.getRoot().getChild(nodeFqn);

      if (node != null)
      {
         Object result = node.get(NODE_OBJECT_KEY);

         if (log.isTraceEnabled() && result != null)
         {
            log.trace(this.toString() + "GateIn user query list found in cache. Query: " + "null::null::null::null::null::null:::" + ";namespace=" + ns);
         }

         return result;
      }

      return null;

   }


   private void invalidateAll()
   {
      boolean success = cache.getRoot().removeChild(getRootNode());

      if (log.isTraceEnabled())
      {
         log.trace(this.toString() + "Invalidating whole cache - success=" + success);
      }
   }


   @Managed
   @ManagedDescription("test to perform some JBC operations in non-JTA environment. See server log once you execute this operation.")
   @Impact(ImpactType.WRITE)
   public void testNonJTA() throws Exception
   {
      log.info("Starting the non-jta test");

      String ns = "idm_realm";

      Object result = getGtnUserLazyPageList(ns);
      log.info("Null object returned (expected null): " + result);

      SimpleObject o1 = new SimpleObject();
      SimpleObject o2 = new SimpleObject();
      log.info("o1=" + o1 + ", o2=" + o2);

      putGtnUserLazyPageList(ns, o1);
      o1.changeState();
      log.info("First object returned (expected o1) : " + getGtnUserLazyPageList(ns));

      putGtnUserLazyPageList(ns, o2);
      o2.changeState();
      log.info("Second object returned (expected o2) : " + getGtnUserLazyPageList(ns));
   }


   @Managed
   @ManagedDescription("test to perform some JBC operations in JTA environment. See server log once you execute this operation.")
   @Impact(ImpactType.WRITE)
   public void testJTA() throws Exception
   {
      log.info("Starting the jta test");

      String ns = "idm_realm";

      // JTA transaction 1
      JTAHelper.beginJTATransaction();

      Object result = getGtnUserLazyPageList(ns);
      log.info("Null object returned (expected null): " + result);

      SimpleObject o1 = new SimpleObject();
      SimpleObject o2 = new SimpleObject();
      log.info("o1=" + o1 + ", o2=" + o2);

      putGtnUserLazyPageList(ns, o1);
      log.info("First object returned (expected o1) : " + getGtnUserLazyPageList(ns));

      // Commit transaction 1. Start transaction 2.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      invalidateAll();
      log.info("Object returned after invalidation (expected null) : " + getGtnUserLazyPageList(ns));

      // Create user (This will enforce some hibernate operations. There is assumption that Hibernate is causing transaction inconsistency)
      createSampleUser("chuann");

      putGtnUserLazyPageList(ns, o2);
      o2.changeState();
      log.info("Second object returned (expected o2) : " + getGtnUserLazyPageList(ns));

      // Commit transaction 2. Start transaction 3.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      // Last lookup
      log.info("Third object returned (expected o2) : " + getGtnUserLazyPageList(ns));

   }


   private Fqn getRootNode()
   {
      return Fqn.fromString("/" + MAIN_ROOT);
   }

   private Fqn getNamespacedFqn(String ns)
   {
      String namespace = ns != null ? ns : NULL_NS_NODE;
      namespace = namespace.replaceAll("/", "_");
      return Fqn.fromString(getRootNode() + "/" + namespace);
   }

   private Fqn getFqn(String ns, String node, Object o)
   {
      return Fqn.fromString(getNamespacedFqn(ns) + "/" + node + "/" + o);
   }

   private void initialize(InputStream jbossCacheConfiguration)
   {
      CacheFactory factory = new DefaultCacheFactory();

      if (jbossCacheConfiguration == null)
      {
         throw new IllegalArgumentException("JBoss Cache configuration InputStream is null");
      }

      this.cache = factory.createCache(jbossCacheConfiguration);

      this.cache.create();
      this.cache.start();

   }

   private void setExpiration(Node node)
   {
      if (expiration != -1 && expiration > 0)
      {
         Long future = new Long(System.currentTimeMillis() + expiration);
         node.put(ExpirationAlgorithmConfig.EXPIRATION_KEY, future);
      }
   }

   @Override
   public void start()
   {
      InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE_LOCATION);
      initialize(inputStream);
      log.info("JBoss cache successfuly started");
   }

   @Override
   public void stop()
   {
   }

   private void createSampleUser(String username)
   {
      // Create user
      User userr = new UserImpl(username);
      userr.setPassword("password");
      userr.setFirstName("johny");
      userr.setLastName("Kikako");
      userr.setEmail("johny@seznam.cz");
      try
      {
         orgService.getUserHandler().createUser(userr, true);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
