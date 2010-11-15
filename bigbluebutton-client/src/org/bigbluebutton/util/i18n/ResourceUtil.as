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
package org.bigbluebutton.util.i18n
{
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.IEventDispatcher;
	import flash.external.ExternalInterface;
	import flash.net.URLLoader;
	import flash.net.URLRequest;
	
	import mx.events.ResourceEvent;
	import mx.resources.IResourceManager;
	import mx.resources.ResourceManager;
	
	import org.bigbluebutton.common.LogUtil;

	public class ResourceUtil extends EventDispatcher {
		private static var instance:ResourceUtil = null;
		public static const LOCALES_FILE:String = "conf/locales.xml";
		private var inited:Boolean = false;
		
		private static var BBB_RESOURCE_BUNDLE:String = 'bbbResources';
		public static var DEFAULT_LANGUAGE:String = "en_US";
		private static var currentLanguage:String = DEFAULT_LANGUAGE;
		private var eventDispatcher:IEventDispatcher;
		
		private var localeChain:Array = new Array();
		
		private var resourceManager:IResourceManager;
		
		public function ResourceUtil(enforcer:SingletonEnforcer) {
			if (enforcer == null) {
				throw new Error( "You Can Only Have One ResourceUtil" );
			}
			initialize();
		}
		
		private function isInited():Boolean {
			return inited;
		}
		
		public function initialize():void {
			// Add a random string on the query so that we always get an up-to-date config.xml
			var date:Date = new Date();
			LogUtil.debug("Loading " + LOCALES_FILE);
			var _urlLoader:URLLoader = new URLLoader();
			_urlLoader.addEventListener(Event.COMPLETE, handleComplete);
			_urlLoader.load(new URLRequest(LOCALES_FILE + "?a=" + date.time));
		}
				
		private function handleComplete(e:Event):void{
			parse(new XML(e.target.data));				
		}
		
		public function parse(xml:XML):void{			
			var list:XMLList = xml.locale;
			LogUtil.debug("--- Supported locales --- \n" + xml.toString() + "\n --- \n");
			var locale:XML;
						
			for each(locale in list){
				localeChain.push(locale.@code);
			}							
			
			resourceManager = ResourceManager.getInstance();
			resourceManager.localeChain = [ExternalInterface.call("getLanguage")];
			var localeAvailable:Boolean = false;
			for (var i:Number = 0; i<localeChain.length; i++){
				if (resourceManager.localeChain[0] == localeChain[i]) localeAvailable = true;
			}
			
			if (!localeAvailable){
				resourceManager.localeChain = [DEFAULT_LANGUAGE];
				changeLocale([DEFAULT_LANGUAGE]);
			} else changeLocale(resourceManager.localeChain[0]);			
			
		}
		
		public static function getInstance():ResourceUtil {
			if (instance == null) {
				LogUtil.debug("Setting up supported locales.");
				instance = new ResourceUtil(new SingletonEnforcer);
			} 
			return instance;
        }
        
        public function changeLocale(... chain):void{        	
        	if(chain != null && chain.length > 0)
        	{
        		var localeURI:String = 'locale/' + chain[0] + '_resources.swf';
        		eventDispatcher = resourceManager.loadResourceModule(localeURI,true);
				localeChain = [chain[0]];
				eventDispatcher.addEventListener(ResourceEvent.COMPLETE, localeChangeComplete);
				eventDispatcher.addEventListener(ResourceEvent.ERROR, handleResourceNotLoaded);
				
				currentLanguage = chain[0];
        	}
        }
        
        private function localeChangeComplete(event:ResourceEvent):void{
        	resourceManager.localeChain = localeChain;
        	update();
        }
        
        /**
         * Defaults to DEFAULT_LANGUAGE when an error is thrown by the ResourceManager 
         * @param event
         */        
        private function handleResourceNotLoaded(event:ResourceEvent):void{
        	resourceManager.localeChain = [DEFAULT_LANGUAGE];
        	update();
        }
        
        public function update():void{
        	dispatchEvent(new Event(Event.CHANGE));
        }
        
        [Bindable("change")]
        public function getString(resourceName:String, parameters:Array = null, locale:String = null):String{
			return resourceManager.getString(BBB_RESOURCE_BUNDLE, resourceName, parameters, locale);
		}
		
		public function getCurrentLanguageCode():String{
			return currentLanguage;
		}
	}
}

class SingletonEnforcer{}