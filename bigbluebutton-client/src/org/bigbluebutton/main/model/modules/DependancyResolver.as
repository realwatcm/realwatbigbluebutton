/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
*/
package org.bigbluebutton.main.model.modules
{
	import flash.utils.Dictionary;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	
	import org.bigbluebutton.common.LogUtil;

	public class DependancyResolver
	{
		private var _modules:Dictionary;
		
		public function DependancyResolver()
		{
		}
		
		/**
		 * Creates a dependency tree for modules using a topological sort algorithm (Khan, 1962, http://portal.acm.org/beta/citation.cfm?doid=368996.369025)
		 */
		public function buildDependencyTree(modules:Dictionary):ArrayCollection{
			this._modules = modules;
			
			var sorted:ArrayCollection = new ArrayCollection();
			var independent:ArrayCollection = getModulesWithNoDependencies();
			
			for (var i:int = 0; i<independent.length; i++) (independent.getItemAt(i) as ModuleDescriptor).resolved = true;
			
			while(independent.length > 0){
				var n:ModuleDescriptor = independent.removeItemAt(0) as ModuleDescriptor;
				sorted.addItem(n);
				
				for (var key:Object in _modules){
					var m:ModuleDescriptor = _modules[key] as ModuleDescriptor;
					m.removeDependancy(n.getName());
					if ((m.unresolvedDependancies.length == 0) && (!m.resolved)){
						independent.addItem(m);
						m.resolved = true;
					}
				}
			}
			
			//Debug Information
			for (var key2:Object in _modules) {
				var m2:ModuleDescriptor = _modules[key2] as ModuleDescriptor;
				if (m2.unresolvedDependancies.length != 0){
					throw new Error("Modules have circular dependancies, please check your config file. Unresolved: " + 
													m2.getName() + " depends on " + m2.unresolvedDependancies.toString());
				}
			}
			LogUtil.debug("Dependency Order: ");
			for (var u:int = 0; u<sorted.length; u++){
				LogUtil.debug(((sorted.getItemAt(u) as ModuleDescriptor).getName()));
				//Alert.show((sorted.getItemAt(u) as ModuleDescriptor).getAttribute("name") as String);
			}

			return sorted;
		}
		
		private function getModulesWithNoDependencies():ArrayCollection{
			var returnArray:ArrayCollection = new ArrayCollection();
			for (var key:Object in _modules) {
				var m:ModuleDescriptor = _modules[key] as ModuleDescriptor;
				if (m.unresolvedDependancies.length == 0) {
					returnArray.addItem(m);
				}
			}
			return returnArray;
		}
	}
}