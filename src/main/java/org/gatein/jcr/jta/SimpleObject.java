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

import java.util.ArrayList;
import java.util.List;

/**
 * Very simple object, which holds some state
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleObject
{
   private int size = -1;
   private List<Object> fullResults = new ArrayList<Object>();

   public void changeState()
   {
      size = size + 2;

      fullResults = new ArrayList<Object>();
      fullResults.add(new Object());
      fullResults.add("Some String: " + size);
   }

   @Override
   public String toString()
   {
      return super.toString() + " size=" + size + ", fullResults=" + fullResults.toString();
   }

}
