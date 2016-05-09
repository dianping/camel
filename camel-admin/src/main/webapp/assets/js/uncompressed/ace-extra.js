if(! ('ace' in window) ) window['ace'] = {}

ace.config = {
 cookie_expiry : 604800, //1 week duration for saved settings
 storage_method: 2 //2 means use cookies, 1 means localStorage, 0 means localStorage if available otherwise cookies
}

ace.settings = {
	is : function(item, status) {
		//such as ace.settings.is('navbar', 'fixed')
		return (ace.data.get('settings', item+'-'+status) == 1)
	},
	exists : function(item, status) {
		return (ace.data.get('settings', item+'-'+status) !== null)
	},
	set : function(item, status) {
		ace.data.set('settings', item+'-'+status, 1)
	},
	unset : function(item, status) {
		ace.data.set('settings', item+'-'+status, -1)
	},
	remove : function(item, status) {
		ace.data.remove('settings', item+'-'+status)
	},

	navbar_fixed : function(fix) {
		fix = fix || false;
		if(!fix && ace.settings.is('sidebar', 'fixed')) {
			ace.settings.sidebar_fixed(false);
		}
		
		var navbar = document.getElementById('navbar');
		if(fix) {
			if(!ace.hasClass(navbar , 'navbar-fixed-top'))  ace.addClass(navbar , 'navbar-fixed-top');
			if(!ace.hasClass(document.body , 'navbar-fixed'))  ace.addClass(document.body , 'navbar-fixed');
			
			ace.settings.set('navbar', 'fixed');
		} else {
			ace.removeClass(navbar , 'navbar-fixed-top');
			ace.removeClass(document.body , 'navbar-fixed');
			
			ace.settings.unset('navbar', 'fixed');
		}
		
		document.getElementById('ace-settings-navbar').checked = fix;
	},


	breadcrumbs_fixed : function(fix) {
		fix = fix || false;
		if(fix && !ace.settings.is('sidebar', 'fixed')) {
			ace.settings.sidebar_fixed(true);
		}

		var breadcrumbs = document.getElementById('breadcrumbs');
		if(fix) {
			if(!ace.hasClass(breadcrumbs , 'breadcrumbs-fixed'))  ace.addClass(breadcrumbs , 'breadcrumbs-fixed');
			if(!ace.hasClass(document.body , 'breadcrumbs-fixed'))  ace.addClass(document.body , 'breadcrumbs-fixed');
			
			ace.settings.set('breadcrumbs', 'fixed');
		} else {
			ace.removeClass(breadcrumbs , 'breadcrumbs-fixed');
			ace.removeClass(document.body , 'breadcrumbs-fixed');
			
			ace.settings.unset('breadcrumbs', 'fixed');
		}
		document.getElementById('ace-settings-breadcrumbs').checked = fix;
	},


	sidebar_fixed : function(fix) {
		fix = fix || false;
		if(!fix && ace.settings.is('breadcrumbs', 'fixed')) {
			ace.settings.breadcrumbs_fixed(false);
		}

		if( fix && !ace.settings.is('navbar', 'fixed') ) {
			ace.settings.navbar_fixed(true);
		}

		var sidebar = document.getElementById('sidebar');
		if(fix) {
			if( !ace.hasClass(sidebar , 'sidebar-fixed') )  ace.addClass(sidebar , 'sidebar-fixed');
			ace.settings.set('sidebar', 'fixed');
		} else {
			ace.removeClass(sidebar , 'sidebar-fixed');
			ace.settings.unset('sidebar', 'fixed');
		}
		document.getElementById('ace-settings-sidebar').checked = fix;
	},

	main_container_fixed : function(inside) {
		inside = inside || false;

		var main_container = document.getElementById('main-container');
		var navbar_container = document.getElementById('navbar-container');
		if(inside) {
			if( !ace.hasClass(main_container , 'container') )  ace.addClass(main_container , 'container');
			if( !ace.hasClass(navbar_container , 'container') )  ace.addClass(navbar_container , 'container');
			ace.settings.set('main-container', 'fixed');
		} else {
			ace.removeClass(main_container , 'container');
			ace.removeClass(navbar_container , 'container');
			ace.settings.unset('main-container', 'fixed');
		}
		document.getElementById('ace-settings-add-container').checked = inside;
		
		
		if(navigator.userAgent.match(/webkit/i)) {
			//webkit has a problem redrawing and moving around the sidebar background in realtime
			//so we do this, to force redraw
			//there will be no problems with webkit if the ".container" class is statically put inside HTML code.
			var sidebar = document.getElementById('sidebar')
			ace.toggleClass(sidebar , 'menu-min')
			setTimeout(function() {	ace.toggleClass(sidebar , 'menu-min') } , 0)
		}
	},

	sidebar_collapsed : function(collpase) {
		collpase = collpase || false;

		var sidebar = document.getElementById('sidebar');
		var icon = document.getElementById('sidebar-collapse').querySelector('[class*="icon-"]');
		var $icon1 = icon.getAttribute('data-icon1');//the icon for expanded state
		var $icon2 = icon.getAttribute('data-icon2');//the icon for collapsed state

		if(collpase) {
			ace.addClass(sidebar , 'menu-min');
			ace.removeClass(icon , $icon1);
			ace.addClass(icon , $icon2);

			ace.settings.set('sidebar', 'collapsed');
		} else {
			ace.removeClass(sidebar , 'menu-min');
			ace.removeClass(icon , $icon2);
			ace.addClass(icon , $icon1);

			ace.settings.unset('sidebar', 'collapsed');
		}

	},
	/**
	select_skin : function(skin) {
	}
	*/
}


//check the status of something
ace.settings.check = function(item, val) {
	if(! ace.settings.exists(item, val) ) return;//no such setting specified
	var status = ace.settings.is(item, val);//is breadcrumbs-fixed? or is sidebar-collapsed? etc
	
	var mustHaveClass = {
		'navbar-fixed' : 'navbar-fixed-top',
		'sidebar-fixed' : 'sidebar-fixed',
		'breadcrumbs-fixed' : 'breadcrumbs-fixed',
		'sidebar-collapsed' : 'menu-min',
		'main-container-fixed' : 'container'
	}


	//if an element doesn't have a specified class, but saved settings say it should, then add it
	//for example, sidebar isn't .fixed, but user fixed it on a previous page
	//or if an element has a specified class, but saved settings say it shouldn't, then remove it
	//for example, sidebar by default is minimized (.menu-min hard coded), but user expanded it and now shouldn't have 'menu-min' class
	
	var target = document.getElementById(item);//#navbar, #sidebar, #breadcrumbs
	if(status != ace.hasClass(target , mustHaveClass[item+'-'+val])) {
		ace.settings[item.replace('-','_')+'_'+val](status);//call the relevant function to mage the changes
	}
}






//save/retrieve data using localStorage or cookie
//method == 1, use localStorage
//method == 2, use cookies
//method not specified, use localStorage if available, otherwise cookies
ace.data_storage = function(method, undefined) {
	var prefix = 'ace.';

	var storage = null;
	var type = 0;
	
	if((method == 1 || method === undefined) && 'localStorage' in window && window['localStorage'] !== null) {
		storage = ace.storage;
		type = 1;
	}
	else if(storage == null && (method == 2 || method === undefined) && 'cookie' in document && document['cookie'] !== null) {
		storage = ace.cookie;
		type = 2;
	}

	//var data = {}
	this.set = function(namespace, key, value, undefined) {
		if(!storage) return;
		
		if(value === undefined) {//no namespace here?
			value = key;
			key = namespace;

			if(value == null) storage.remove(prefix+key)
			else {
				if(type == 1)
					storage.set(prefix+key, value)
				else if(type == 2)
					storage.set(prefix+key, value, ace.config.cookie_expiry)
			}
		}
		else {
			if(type == 1) {//localStorage
				if(value == null) storage.remove(prefix+namespace+'.'+key)
				else storage.set(prefix+namespace+'.'+key, value);
			}
			else if(type == 2) {//cookie
				var val = storage.get(prefix+namespace);
				var tmp = val ? JSON.parse(val) : {};

				if(value == null) {
					delete tmp[key];//remove
					if(ace.sizeof(tmp) == 0) {//no other elements in this cookie, so delete it
						storage.remove(prefix+namespace);
						return;
					}
				}
				
				else {
					tmp[key] = value;
				}

				storage.set(prefix+namespace , JSON.stringify(tmp), ace.config.cookie_expiry)
			}
		}
	}

	this.get = function(namespace, key, undefined) {
		if(!storage) return null;
		
		if(key === undefined) {//no namespace here?
			key = namespace;
			return storage.get(prefix+key);
		}
		else {
			if(type == 1) {//localStorage
				return storage.get(prefix+namespace+'.'+key);
			}
			else if(type == 2) {//cookie
				var val = storage.get(prefix+namespace);
				var tmp = val ? JSON.parse(val) : {};
				return key in tmp ? tmp[key] : null;
			}
		}
	}

	
	this.remove = function(namespace, key, undefined) {
		if(!storage) return;
		
		if(key === undefined) {
			key = namespace
			this.set(key, null);
		}
		else {
			this.set(namespace, key, null);
		}
	}
}





//cookie storage
ace.cookie = {
	// The following functions are from Cookie.js class in TinyMCE, Moxiecode, used under LGPL.

	/**
	 * Get a cookie.
	 */
	get : function(name) {
		var cookie = document.cookie, e, p = name + "=", b;

		if ( !cookie )
			return;

		b = cookie.indexOf("; " + p);

		if ( b == -1 ) {
			b = cookie.indexOf(p);

			if ( b != 0 )
				return null;

		} else {
			b += 2;
		}

		e = cookie.indexOf(";", b);

		if ( e == -1 )
			e = cookie.length;

		return decodeURIComponent( cookie.substring(b + p.length, e) );
	},

	/**
	 * Set a cookie.
	 *
	 * The 'expires' arg can be either a JS Date() object set to the expiration date (back-compat)
	 * or the number of seconds until expiration
	 */
	set : function(name, value, expires, path, domain, secure) {
		var d = new Date();

		if ( typeof(expires) == 'object' && expires.toGMTString ) {
			expires = expires.toGMTString();
		} else if ( parseInt(expires, 10) ) {
			d.setTime( d.getTime() + ( parseInt(expires, 10) * 1000 ) ); // time must be in miliseconds
			expires = d.toGMTString();
		} else {
			expires = '';
		}

		document.cookie = name + "=" + encodeURIComponent(value) +
			((expires) ? "; expires=" + expires : "") +
			((path) ? "; path=" + path : "") +
			((domain) ? "; domain=" + domain : "") +
			((secure) ? "; secure" : "");
	},

	/**
	 * Remove a cookie.
	 *
	 * This is done by setting it to an empty value and setting the expiration time in the past.
	 */
	remove : function(name, path) {
		this.set(name, '', -1000, path);
	}
};


//local storage
ace.storage = {
	get: function(key) {
		return window['localStorage'].getItem(key);
	},
	set: function(key, value) {
		window['localStorage'].setItem(key , value);
	},
	remove: function(key) {
		window['localStorage'].removeItem(key);
	}
};






//count the number of properties in an object
//useful for getting the number of elements in an associative array
ace.sizeof = function(obj) {
	var size = 0;
	for(var key in obj) if(obj.hasOwnProperty(key)) size++;
	return size;
}

//because jQuery may not be loaded at this stage, we use our own toggleClass
ace.hasClass = function(elem, className) {
	return (" " + elem.className + " ").indexOf(" " + className + " ") > -1;
}
ace.addClass = function(elem, className) {
 if (!ace.hasClass(elem, className)) {
	var currentClass = elem.className;
	elem.className = currentClass + (currentClass.length? " " : "") + className;
 }
}
ace.removeClass = function(elem, className) {ace.replaceClass(elem, className);}

ace.replaceClass = function(elem, className, newClass) {
	var classToRemove = new RegExp(("(^|\\s)" + className + "(\\s|$)"), "i");
	elem.className = elem.className.replace(classToRemove, function (match, p1, p2) {
		return newClass? (p1 + newClass + p2) : " ";
	}).replace(/^\s+|\s+$/g, "");
}

ace.toggleClass = function(elem, className) {
	if(ace.hasClass(elem, className))
		ace.removeClass(elem, className);
	else ace.addClass(elem, className);
}




//data_storage instance used inside ace.settings etc
ace.data = new ace.data_storage(ace.config.storage_method);
