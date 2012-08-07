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
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

/**
 * Simple component for testing JCR behaviour in JTA/non-JTA environment
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Managed
@ManagedDescription("JCRTransactionTest")
@NameTemplate({
      @Property(key = "name", value = "JCRTransactionTest"),
      @Property(key = "service", value = "JCRTransactionTest")
})
public class JCRTransactionTest implements Startable
{
   private static final Logger log = LoggerFactory.getLogger(JCRTransactionTest.class);
   private RepositoryService repositoryService;

   public JCRTransactionTest(RepositoryService repositoryService)
   {
      this.repositoryService = repositoryService;
   }

   @Managed
   @ManagedDescription("test to perform some JCR operations in non-JTA environment. See server log once you execute this operation.")
   @Impact(ImpactType.WRITE)
   public void testNonJTA() throws Exception
   {
      log.info("Starting the non-jta test");

      // Obtain JCR session and base node
      ManageableRepository repo = repositoryService.getDefaultRepository();
      Session session = repo.getSystemSession("portal-work");
      Node parentNode = (Node)session.getItem("/");
      Node testNode;
      if (parentNode.hasNode("test"))
      {
         testNode = parentNode.getNode("test");
      }
      else
      {
         testNode = parentNode.addNode("test", "nt:folder");
      }

      // Add node and save JCR session
      Node aNode = testNode.addNode("a", "nt:folder");
      log.info("Node '/test/a' created in JCR workspace");
      session.save();
      log.info("JCR session saved");

      // Execute JCR query. New node is in results as expected
      executeTestQuery(session);

      // Remove node and save JCR session
      aNode.remove();
      log.info("Node '/test/a' deleted from JCR workspace");
      session.save();
      log.info("JCR session saved");

      // Execute JCR query. Node was deleted and it's not in results anymore as expected
      executeTestQuery(session);

      session.logout();
   }

   @Managed
   @ManagedDescription("test to perform some JCR operations in JTA environment. See server log once you execute this operation.")
   @Impact(ImpactType.WRITE)
   public void testJTA() throws Exception
   {
      log.info("Starting the jta test");

      // JTA transaction 1
      JTAHelper.beginJTATransaction();

      // Obtain JCR session and base node
      ManageableRepository repo = repositoryService.getDefaultRepository();
      Session session = repo.getSystemSession("portal-work");
      Node parentNode = (Node)session.getItem("/");
      Node testNode;
      if (parentNode.hasNode("test"))
      {
         testNode = parentNode.getNode("test");
      }
      else
      {
         testNode = parentNode.addNode("test", "nt:folder");
      }

      // Add node and save JCR session
      Node aNode = testNode.addNode("a", "nt:folder");
      log.info("Node '/test/a' created in JCR workspace");
      session.save();
      log.info("JCR session saved");

      // Execute JCR query. New node is NOT here!! (I expect it to be here since it was added in same JTA transaction)
      executeTestQuery(session);

      // Commit transaction 1. Start transaction 2.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      // Execute JCR query. Now node is covered in the results
      executeTestQuery(session);

      // Remove node and save JCR session
      aNode.remove();
      log.info("Node '/test/a' deleted from JCR workspace");
      session.save();
      log.info("JCR session saved");

      // Execute JCR query. Node is still here!! (I don't expect it to be here since it was deleted in this JTA transaction)
      executeTestQuery(session);

      // Commit transaction 2. Start transaction 3.
      JTAHelper.finishJTATransaction();
      JTAHelper.beginJTATransaction();

      // Now node is deleted as expected
      executeTestQuery(session);

      JTAHelper.finishJTATransaction();

      session.logout();
   }

   @Managed
   @ManagedDescription("test to simulate with setRollbackOnly. See server log once you execute this operation.")
   @Impact(ImpactType.WRITE)
   public void testRollback() throws Exception
   {
      log.info("Starting the test for JTA rollback");
      JTAHelper.beginJTATransaction();

      // Obtain JCR session and base node
      ManageableRepository repo = repositoryService.getDefaultRepository();
      Session session = repo.getSystemSession("portal-work");

      Node parentNode = (Node)session.getItem("/");
      Node testNode;
      if (parentNode.hasNode("test"))
      {
         testNode = parentNode.getNode("test");
      }
      else
      {
         testNode = parentNode.addNode("test", "nt:folder");
      }

      // Add node and save JCR session
      testNode.addNode("a", "nt:folder");
      log.info("Node '/test/a' created in JCR workspace");

      // mark tx for rollback
      JTAHelper.getUserTransaction().setRollbackOnly();

      session.save();
      log.info("JCR session saved");

      JTAHelper.finishJTATransaction();
   }

   private void executeTestQuery(Session session) throws Exception
   {
      QueryManager queryMgr = session.getWorkspace().getQueryManager();
      Query query = queryMgr.createQuery("SELECT * FROM nt:folder WHERE jcr:path LIKE '/test/%'", Query.SQL);
      QueryResult queryResult = query.execute();
      log.info("Number of subnodes of node '/test': " + queryResult.getNodes().getSize());
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
