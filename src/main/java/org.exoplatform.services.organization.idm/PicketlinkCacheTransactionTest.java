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

package org.exoplatform.services.organization.idm;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.jcr.jta.JCRTransactionTest;
import org.gatein.jcr.jta.JTAHelper;
import org.gatein.jcr.jta.SimpleObject;
import org.picocontainer.Startable;

/**
 * Test of Picketlink cache in JTA environment
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Managed
@ManagedDescription("PicketlinkCacheTransactionTest")
@NameTemplate({
      @Property(key = "name", value = "PicketlinkCacheTransactionTest"),
      @Property(key = "service", value = "PicketlinkCacheTransactionTest")
})
public class PicketlinkCacheTransactionTest implements Startable
{

   private static final Logger log = LoggerFactory.getLogger(JCRTransactionTest.class);
   private PicketLinkIDMOrganizationServiceImpl orgService;
   private PicketLinkIDMServiceImpl plIdmService;

   public PicketlinkCacheTransactionTest(OrganizationService orgService, PicketLinkIDMService plIdmService)
   {
      this.orgService = (PicketLinkIDMOrganizationServiceImpl)orgService;
      this.plIdmService = (PicketLinkIDMServiceImpl)plIdmService;
   }


   @Managed
   @ManagedDescription("test to perform some ploperations in JTA environment. See server log once you execute this operation.")
   @Impact(ImpactType.WRITE)
   public void testJTA() throws Exception
   {
      log.info("Starting the jta test");

      String ns = "idm_realm";

      // JTA transaction 1
      JTAHelper.beginJTATransaction();

      IDMUserListAccess result = plIdmService.getIntegrationCache().getGtnUserLazyPageList(ns, new Query());
      log.info("Null object returned (expected null): " + result);

      // Refresh list and save it to cache
      PageList<User> pageList = orgService.getUserHandler().findUsers(new Query());
      pageList.currentPage();
      result = plIdmService.getIntegrationCache().getGtnUserLazyPageList(ns, new Query());
      log.info("First object returned : " + result + ", " + result.getSize());

      // Commit transaction 1. Start transaction 2.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      // Create user
      User userr = new UserImpl("chuanito");
      userr.setPassword("password");
      userr.setFirstName("johny");
      userr.setLastName("Kikako");
      userr.setEmail("johny@seznam.cz");
      orgService.getUserHandler().createUser(userr, true);

      // Refresh list and save it to cache
      pageList = orgService.getUserHandler().findUsers(new Query());
      pageList.currentPage();
      result = plIdmService.getIntegrationCache().getGtnUserLazyPageList(ns, new Query());
      log.info("Second object returned : " + result + ", " + result.getSize());

      // Commit transaction 2. Start transaction 3.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      // Delete user
      orgService.getUserHandler().removeUser("chuanito", true);

      // Refresh list and save it to cache
      //pageList = orgService.getUserHandler().findUsers(new Query());
      ListAccess<User> la = orgService.getUserHandler().findUsersByQuery(new Query());
      //pageList.currentPage();
      result = plIdmService.getIntegrationCache().getGtnUserLazyPageList(ns, new Query());
      //log.info("Third object returned : " + result + ", " + result.getSize());
      log.info("Third object returned : " + result);

      // Commit transaction 2. Start transaction 3.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      // Last lookup
      result = plIdmService.getIntegrationCache().getGtnUserLazyPageList(ns, new Query());
      log.info("Fourth object returned : " + result + ", " + result.getSize());
   }


   @Override
   public void start()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void stop()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }
}
