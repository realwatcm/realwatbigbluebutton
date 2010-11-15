/*
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package local.server;


import org.zoolu.sip.address.SipURL;
import org.zoolu.net.SocketAddress;


/** DomainRoutingRule.
  */
class DomainRoutingRule implements RoutingRule
{

   /** Matching domain. */
   String domain;

   /** Next-hop server. */
   SocketAddress nexthop;
   
   
   /** Creates a new DomainRoutingRule. */
   public DomainRoutingRule(String domain, SocketAddress nexthop)
   {  this.domain=domain;
      this.nexthop=nexthop;
   }
   

   /** Gets the proper next-hop SipURL for the selected URL.
     * It return the SipURL used to reach the selected URL.
     * @param sip_url the selected destination URL
     * @return returns the proper next-hop SipURL for the selected URL
     * if the routing rule matches the URL, otherwise it returns null. */
   public SipURL getNexthop(SipURL sip_url)
   {  String host=sip_url.getHost();
      if ((host.equalsIgnoreCase(domain)))
      {  return new SipURL(sip_url.getUserName(),nexthop.getAddress().toString(),nexthop.getPort());
      }
      else return null;
   }

   /** Gets the String value. */
   public String toString()
   {  return "{domain="+domain+","+"nexthop="+nexthop+"}";
   }
}  