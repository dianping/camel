if(! ('ace' in window) ) window['ace'] = {}
jQuery(function() {
	//at some places we try to use 'tap' event instead of 'click' if jquery mobile plugin is available
	window['ace'].click_event = $.fn.tap ? "tap" : "click";
});

(function($ , undefined) {
	var multiplible = 'multiple' in document.createElement('INPUT');
	var hasFileList = 'FileList' in window;//file list enabled in modern browsers
	var hasFileReader = 'FileReader' in window;

	var Ace_File_Input = function(element , settings) {
		var self = this;
		this.settings = $.extend({}, $.fn.ace_file_input.defaults, settings);

		this.$element = $(element);
		this.element = element;
		this.disabled = false;
		this.can_reset = true;

		this.$element.on('change.ace_inner_call', function(e , ace_inner_call){
			if(ace_inner_call === true) return;//this change event is called from above drop event
			return handle_on_change.call(self);
		});
		
		this.$element.wrap('<div class="ace-file-input" />');
		
		this.apply_settings();
	}
	Ace_File_Input.error = {
		'FILE_LOAD_FAILED' : 1,
		'IMAGE_LOAD_FAILED' : 2,
		'THUMBNAIL_FAILED' : 3
	};


	Ace_File_Input.prototype.apply_settings = function() {
		var self = this;
		var remove_btn = !!this.settings.icon_remove;

		this.multi = this.$element.attr('multiple') && multiplible;
		this.well_style = this.settings.style == 'well';

		if(this.well_style) this.$element.parent().addClass('ace-file-multiple');
		 else this.$element.parent().removeClass('ace-file-multiple');

		this.$element.parent().find(':not(input[type=file])').remove();//remove all except our input, good for when changing settings
		this.$element.after('<label class="file-label" data-title="'+this.settings.btn_choose+'"><span class="file-name" data-title="'+this.settings.no_file+'">'+(this.settings.no_icon ? '<i class="'+this.settings.no_icon+'"></i>' : '')+'</span></label>'+(remove_btn ? '<a class="remove" href="#"><i class="'+this.settings.icon_remove+'"></i></a>' : ''));
		this.$label = this.$element.next();

		this.$label.on('click', function(){//firefox mobile doesn't allow 'tap'!
			if(!this.disabled && !self.element.disabled && !self.$element.attr('readonly')) 
				self.$element.click();
		})

		if(remove_btn) this.$label.next('a').on(ace.click_event, function(){
			if(! self.can_reset ) return false;
			
			var ret = true;
			if(self.settings.before_remove) ret = self.settings.before_remove.call(self.element);
			if(!ret) return false;
			return self.reset_input();
		});


		if(this.settings.droppable && hasFileList) {
			enable_drop_functionality.call(this);
		}
	}

	Ace_File_Input.prototype.show_file_list = function($files) {
		var files = typeof $files === "undefined" ? this.$element.data('ace_input_files') : $files;
		if(!files || files.length == 0) return;

		//////////////////////////////////////////////////////////////////

		if(this.well_style) {
			this.$label.find('.file-name').remove();
			if(!this.settings.btn_change) this.$label.addClass('hide-placeholder');
		}
		this.$label.attr('data-title', this.settings.btn_change).addClass('selected');
		
		for (var i = 0; i < files.length; i++) {
			var filename = typeof files[i] === "string" ? files[i] : $.trim( files[i].name );
			var index = filename.lastIndexOf("\\") + 1;
			if(index == 0)index = filename.lastIndexOf("/") + 1;
			filename = filename.substr(index);
			
			var fileIcon = 'icon-file';
			if((/\.(jpe?g|png|gif|svg|bmp|tiff?)$/i).test(filename)) {
				fileIcon = 'icon-picture';
			}
			else if((/\.(mpe?g|flv|mov|avi|swf|mp4|mkv|webm|wmv|3gp)$/i).test(filename)) fileIcon = 'icon-film';
			else if((/\.(mp3|ogg|wav|wma|amr|aac)$/i).test(filename)) fileIcon = 'icon-music';


			if(!this.well_style) this.$label.find('.file-name').attr({'data-title':filename}).find('[class*="icon-"]').attr('class', fileIcon);
			else {
				this.$label.append('<span class="file-name" data-title="'+filename+'"><i class="'+fileIcon+'"></i></span>');
				var type = $.trim(files[i].type);
				var can_preview = hasFileReader && this.settings.thumbnail 
						&&
						( (type.length > 0 && type.match('image')) || (type.length == 0 && fileIcon == 'icon-picture') )//the second one is for Android's default browser which gives an empty text for file.type
				if(can_preview) {
					var self = this;
					$.when(preview_image.call(this, files[i])).fail(function(result){
						//called on failure to load preview
						if(self.settings.preview_error) self.settings.preview_error.call(self, filename, result.code);
					});
				}
			}

		}

		return true;
	}

	Ace_File_Input.prototype.reset_input = function() {
	  this.$label.attr({'data-title':this.settings.btn_choose, 'class':'file-label'})
			.find('.file-name:first').attr({'data-title':this.settings.no_file , 'class':'file-name'})
			.find('[class*="icon-"]').attr('class', this.settings.no_icon)
			.prev('img').remove();
			if(!this.settings.no_icon) this.$label.find('[class*="icon-"]').remove();
		
		this.$label.find('.file-name').not(':first').remove();
		
		if(this.$element.data('ace_input_files')) {
			this.$element.removeData('ace_input_files');
			this.$element.removeData('ace_input_method');
		}

		this.reset_input_field();
		
		return false;
	}

	Ace_File_Input.prototype.reset_input_field = function() {
		//http://stackoverflow.com/questions/1043957/clearing-input-type-file-using-jquery/13351234#13351234
		this.$element.wrap('<form>').closest('form').get(0).reset();
		this.$element.unwrap();
	}
	
	Ace_File_Input.prototype.enable_reset = function(can_reset) {
		this.can_reset = can_reset;
	}

	Ace_File_Input.prototype.disable = function() {
		this.disabled = true;
		this.$element.attr('disabled', 'disabled').addClass('disabled');
	}
	Ace_File_Input.prototype.enable = function() {
		this.disabled = false;
		this.$element.removeAttr('disabled').removeClass('disabled');
	}
	
	Ace_File_Input.prototype.files = function() {
		return $(this).data('ace_input_files') || null;
	}
	Ace_File_Input.prototype.method = function() {
		return $(this).data('ace_input_method') || '';
	}
	
	Ace_File_Input.prototype.update_settings = function(new_settings) {
		this.settings = $.extend({}, this.settings, new_settings);
		this.apply_settings();
	}



	var enable_drop_functionality = function() {
		var self = this;
		var dropbox = this.element.parentNode;		
		$(dropbox).on('dragenter', function(e){
			e.preventDefault();
			e.stopPropagation();
		}).on('dragover', function(e){
			e.preventDefault();
			e.stopPropagation();
		}).on('drop', function(e){
			e.preventDefault();
			e.stopPropagation();

			var dt = e.originalEvent.dataTransfer;
			var files = dt.files;
			if(!self.multi && files.length > 1) {//single file upload, but dragged multiple files
				var tmpfiles = [];
				tmpfiles.push(files[0]);
				files = tmpfiles;//keep only first file
			}
			
			var ret = true;
			if(self.settings.before_change) ret = self.settings.before_change.call(self.element, files, true);//true means files have been dropped
			if(!ret || ret.length == 0) {
				return false;
			}
			
			//user can return a modified File Array as result
			if(ret instanceof Array || (hasFileList && ret instanceof FileList)) files = ret;
			
			
			self.$element.data('ace_input_files', files);//save files data to be used later by user
			self.$element.data('ace_input_method', 'drop');


			self.show_file_list(files);
			
			
			self.$element.triggerHandler('change' , [true]);//true means inner_call
			return true;
		});
	}
	
	
	var handle_on_change = function() {
		var ret = true;
		if(this.settings.before_change) ret = this.settings.before_change.call(this.element, this.element.files || [this.element.value]/*make it an array*/, false);//false means files have been selected, not dropped
		if(!ret || ret.length == 0) {
			if(!this.$element.data('ace_input_files')) this.reset_input_field();//if nothing selected before, reset because of the newly unacceptable (ret=false||length=0) selection
			return false;
		}
		

		//user can return a modified File Array as result
		var files = !hasFileList ? null ://for old IE, etc
					((ret instanceof Array || ret instanceof FileList) ? ret : this.element.files);
		this.$element.data('ace_input_method', 'select');


		if(files && files.length > 0) {//html5
			this.$element.data('ace_input_files', files);
		}
		else {
			var name = $.trim( this.element.value );
			if(name && name.length > 0) {
				files = []
				files.push(name);
				this.$element.data('ace_input_files', files);
			}
		}

		if(!files || files.length == 0) return false;
		this.show_file_list(files);

		return true;
	}




	var preview_image = function(file) {
		var self = this;
		var $span = self.$label.find('.file-name:last');//it should be out of onload, otherwise all onloads may target the same span because of delays
		
		var deferred = new $.Deferred
		var reader = new FileReader();
		reader.onload = function (e) {
			$span.prepend("<img class='middle' style='display:none;' />");
			var img = $span.find('img:last').get(0);

			$(img).one('load', function() {
				//if image loaded successfully
				var size = 50;
				if(self.settings.thumbnail == 'large') size = 150;
				else if(self.settings.thumbnail == 'fit') size = $span.width();
				$span.addClass(size > 50 ? 'large' : '');

				var thumb = get_thumbnail(img, size, file.type);
				if(thumb == null) {
					//if making thumbnail fails
					$(this).remove();
					deferred.reject({code:Ace_File_Input.error['THUMBNAIL_FAILED']});
					return;
				}

				var w = thumb.w, h = thumb.h;
				if(self.settings.thumbnail == 'small') {w=h=size;};
				$(img).css({'background-image':'url('+thumb.src+')' , width:w, height:h})									
						.data('thumb', thumb.src)
						.attr({src:'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg=='})
						.show()

				///////////////////
				deferred.resolve();
			}).one('error', function() {
				//for example when a file has image extenstion, but format is something else
				$span.find('img').remove();
				deferred.reject({code:Ace_File_Input.error['IMAGE_LOAD_FAILED']});
			});

			img.src = e.target.result;
		}
		reader.onerror = function (e) {
			deferred.reject({code:Ace_File_Input.error['FILE_LOAD_FAILED']});
		}
		reader.readAsDataURL(file);

		return deferred.promise();
	}

	var get_thumbnail = function(img, size, type) {
		
		var w = img.width, h = img.height;
		if(w > size || h > size) {
		  if(w > h) {
			h = parseInt(size/w * h);
			w = size;
		  } else {
			w = parseInt(size/h * w);
			h = size;
		  }
		}

		var dataURL
		try {
			var canvas = document.createElement('canvas');
			canvas.width = w; canvas.height = h;
			var context = canvas.getContext('2d');
			context.drawImage(img, 0, 0, img.width, img.height, 0, 0, w, h);
			dataURL = canvas.toDataURL(/*type == 'image/jpeg' ? type : 'image/png', 10*/)
		} catch(e) {
			dataURL = null;
		}

		//there was only one image that failed in firefox completely randomly! so let's double check it
		if(!( /^data\:image\/(png|jpe?g|gif);base64,[0-9A-Za-z\+\/\=]+$/.test(dataURL)) ) dataURL = null;
		if(! dataURL) return null;

		return {src: dataURL, w:w, h:h};
	}



	///////////////////////////////////////////
	$.fn.ace_file_input = function (option,value) {
		var retval;

		var $set = this.each(function () {
			var $this = $(this);
			var data = $this.data('ace_file_input');
			var options = typeof option === 'object' && option;

			if (!data) $this.data('ace_file_input', (data = new Ace_File_Input(this, options)));
			if (typeof option === 'string') retval = data[option](value);
		});

		return (retval === undefined) ? $set : retval;
	};


	$.fn.ace_file_input.defaults = {
		style:false,
		no_file:'No File ...',
		no_icon:'icon-upload-alt',
		btn_choose:'Choose',
		btn_change:'Change',
		icon_remove:'icon-remove',
		droppable:false,
		thumbnail:false,//large, fit, small
		
		//callbacks
		before_change:null,
		before_remove:null,
		preview_error:null
     }


})(window.jQuery);








(function($ , undefined) {
	$.fn.ace_spinner = function(options) {
		
		//when min is negative, the input maxlength does not account for the extra minus sign
		this.each(function() {
			var icon_up = options.icon_up || 'icon-chevron-up'
			var icon_down = options.icon_down || 'icon-chevron-down'
			var on_sides = options.on_sides || false
			
			var btn_up_class = options.btn_up_class || ''
			var btn_down_class = options.btn_down_class || ''
		
			var max = options.max || 999
			max = (''+max).length
			
				$(this).addClass('spinner-input form-control').wrap('<div class="ace-spinner">')
				var $parent_div = $(this).closest('.ace-spinner').spinner(options).wrapInner("<div class='input-group'></div>")

				if(on_sides)
				{
				  $(this).before('<div class="spinner-buttons input-group-btn">\
							<button type="button" class="btn spinner-down btn-xs '+btn_down_class+'">\
								<i class="'+icon_down+'"></i>\
							</button>\
						</div>')
				  .after('<div class="spinner-buttons input-group-btn">\
							<button type="button" class="btn spinner-up btn-xs '+btn_up_class+'">\
								<i class="'+icon_up+'"></i>\
							</button>\
						</div>')
				
					$parent_div.addClass('touch-spinner')
					$parent_div.css('width' , (max * 20 + 40)+'px')
				}
				else {
					 $(this).after('<div class="spinner-buttons input-group-btn">\
							<button type="button" class="btn spinner-up btn-xs '+btn_up_class+'">\
								<i class="'+icon_up+'"></i>\
							</button>\
							<button type="button" class="btn spinner-down btn-xs '+btn_down_class+'">\
								<i class="'+icon_down+'"></i>\
							</button>\
						</div>')

					if("ontouchend" in document || options.touch_spinner) {
						$parent_div.addClass('touch-spinner')
						$parent_div.css('width' , (max * 20 + 40)+'px')
					}
					else {
						$(this).next().addClass('btn-group-vertical');
						$parent_div.css('width' , (max * 20 + 10)+'px')
					}
				}
				
				

			$(this).on('mousewheel DOMMouseScroll', function(event){
				var delta = event.originalEvent.detail < 0 || event.originalEvent.wheelDelta > 0 ? 1 : -1
				$parent_div.spinner('step', delta > 0)//accepts true or false as second param
				$parent_div.spinner('triggerChangedEvent')
				return false
			});
			var that = $(this);
			$parent_div.on('changed', function(){
				that.trigger('change')//trigger the input's change event
			});
			
		});
		
		return this;
	}


})(window.jQuery);






(function($ , undefined) {
	$.fn.ace_wizard = function(options) {
		
		this.each(function() {
			var $this = $(this);
			$this.wizard();

			var buttons = $this.siblings('.wizard-actions').eq(0);
			var $wizard = $this.data('wizard');
			$wizard.$prevBtn.remove();
			$wizard.$nextBtn.remove();
			
			$wizard.$prevBtn = buttons.find('.btn-prev').eq(0).on(ace.click_event,  function(){
				$this.wizard('previous');
			}).attr('disabled', 'disabled');
			$wizard.$nextBtn = buttons.find('.btn-next').eq(0).on(ace.click_event,  function(){
				$this.wizard('next');
			}).removeAttr('disabled');
			$wizard.nextText = $wizard.$nextBtn.text();
		});
		
		return this;
	}

})(window.jQuery);





(function($ , undefined) {
	$.fn.ace_colorpicker = function(options) {
		
		var settings = $.extend( {
			pull_right:false,
			caret:true
        }, options);
		
		this.each(function() {
		
			var $that = $(this);
			var colors = '';
			var color = '';
			$(this).hide().find('option').each(function() {
				var $class = 'colorpick-btn';
				if(this.selected) {
					$class += ' selected';
					color = this.value;
				}
				colors += '<li><a class="'+$class+'" href="#" style="background-color:'+this.value+';" data-color="'+this.value+'"></a></li>';
			}).end().on('change.ace_inner_call', function(){
					$(this).next().find('.btn-colorpicker').css('background-color', this.value);
			})
			.after('<div class="dropdown dropdown-colorpicker"><a data-toggle="dropdown" class="dropdown-toggle" href="#"><span class="btn-colorpicker" style="background-color:'+color+'"></span></a><ul class="dropdown-menu'+(settings.caret? ' dropdown-caret' : '')+(settings.pull_right ? ' pull-right' : '')+'">'+colors+'</ul></div>')
			.next().find('.dropdown-menu').on(ace.click_event, function(e) {
				var a = $(e.target);
				if(!a.is('.colorpick-btn')) return false;
				a.closest('ul').find('.selected').removeClass('selected');
				a.addClass('selected');
				var color = a.data('color');

				$that.val(color).change();

				e.preventDefault();
				return true;//if false, dropdown won't hide!
			});
			
			
		});
		return this;
		
	}	
	
	
})(window.jQuery);












(function($ , undefined) {
	$.fn.ace_tree = function(options) {
		var $options = {
			'open-icon' : 'icon-folder-open',
			'close-icon' : 'icon-folder-close',
			'selectable' : true,
			'selected-icon' : 'icon-ok',
			'unselected-icon' : 'tree-dot'
		}
		
		$options = $.extend({}, $options, options)

		this.each(function() {
			var $this = $(this);
			$this.html('<div class = "tree-folder" style="display:none;">\
				<div class="tree-folder-header">\
					<i class="'+$options['close-icon']+'"></i>\
					<div class="tree-folder-name"></div>\
				</div>\
				<div class="tree-folder-content"></div>\
				<div class="tree-loader" style="display:none"></div>\
			</div>\
			<div class="tree-item" style="display:none;">\
				'+($options['unselected-icon'] == null ? '' : '<i class="'+$options['unselected-icon']+'"></i>')+'\
				<div class="tree-item-name"></div>\
			</div>');
			$this.addClass($options['selectable'] == true ? 'tree-selectable' : 'tree-unselectable');
			
			$this.tree($options);
		});

		return this;
	}


})(window.jQuery);












(function($ , undefined) {
	$.fn.ace_wysiwyg = function($options , undefined) {
		var options = $.extend( {
			speech_button:true,
			wysiwyg:{}
        }, $options);

		var color_values = [
			'#ac725e','#d06b64','#f83a22','#fa573c','#ff7537','#ffad46',
			'#42d692','#16a765','#7bd148','#b3dc6c','#fbe983','#fad165',
			'#92e1c0','#9fe1e7','#9fc6e7','#4986e7','#9a9cff','#b99aff',
			'#c2c2c2','#cabdbf','#cca6ac','#f691b2','#cd74e6','#a47ae2',
			'#444444'
		]

		var button_defaults =
		{
			'font' : {
				values:['Arial', 'Courier', 'Comic Sans MS', 'Helvetica', 'Open Sans', 'Tahoma', 'Verdana'],
				icon:'icon-font',
				title:'Font'
			},
			'fontSize' : {
				values:{5:'Huge', 3:'Normal', 1:'Small'},
				icon:'icon-text-height',
				title:'Font Size'
			},
			'bold' : {
				icon : 'icon-bold',
				title : 'Bold (Ctrl/Cmd+B)'
			},
			'italic' : {
				icon : 'icon-italic',
				title : 'Italic (Ctrl/Cmd+I)'
			},
			'strikethrough' : {
				icon : 'icon-strikethrough',
				title : 'Strikethrough'
			},
			'underline' : {
				icon : 'icon-underline',
				title : 'Underline'
			},
			'insertunorderedlist' : {
				icon : 'icon-list-ul',
				title : 'Bullet list'
			},
			'insertorderedlist' : {
				icon : 'icon-list-ol',
				title : 'Number list'
			},
			'outdent' : {
				icon : 'icon-indent-left',
				title : 'Reduce indent (Shift+Tab)'
			},
			'indent' : {
				icon : 'icon-indent-right',
				title : 'Indent (Tab)'
			},
			'justifyleft' : {
				icon : 'icon-align-left',
				title : 'Align Left (Ctrl/Cmd+L)'
			},
			'justifycenter' : {
				icon : 'icon-align-center',
				title : 'Center (Ctrl/Cmd+E)'
			},
			'justifyright' : {
				icon : 'icon-align-right',
				title : 'Align Right (Ctrl/Cmd+R)'
			},
			'justifyfull' : {
				icon : 'icon-align-justify',
				title : 'Justify (Ctrl/Cmd+J)'
			},
			'createLink' : {
				icon : 'icon-link',
				title : 'Hyperlink',
				button_text : 'Add',
				placeholder : 'URL',
				button_class : 'btn-primary'
			},
			'unlink' : {
				icon : 'icon-unlink',
				title : 'Remove Hyperlink'
			},
			'insertImage' : {
				icon : 'icon-picture',
				title : 'Insert picture',
				button_text : '<i class="icon-file"></i> Choose Image &hellip;',
				placeholder : 'Image URL',
				button_insert : 'Insert',
				button_class : 'btn-success',
				button_insert_class : 'btn-primary',
				choose_file: true //show the choose file button?
			},
			'foreColor' : {
				values : color_values,
				title : 'Change Color'
			},
			'backColor' : {
				values : color_values,
				title : 'Change Background Color'
			},
			'undo' : {
				icon : 'icon-undo',
				title : 'Undo (Ctrl/Cmd+Z)'
			},
			'redo' : {
				icon : 'icon-repeat',
				title : 'Redo (Ctrl/Cmd+Y)'
			},
			'viewSource' : {
				icon : 'icon-code',
				title : 'View Source'
			}
		}
		
		var toolbar_buttons =
		options.toolbar ||
		[
			'font',
			null,
			'fontSize',
			null,
			'bold',
			'italic',
			'strikethrough',
			'underline',
			null,
			'insertunorderedlist',
			'insertorderedlist',
			'outdent',
			'indent',
			null,
			'justifyleft',
			'justifycenter',
			'justifyright',
			'justifyfull',
			null,
			'createLink',
			'unlink',
			null,
			'insertImage',
			null,
			'foreColor',
			null,
			'undo',
			'redo',
			null,
			'viewSource'
		]


		this.each(function() {
			var toolbar = ' <div class="wysiwyg-toolbar btn-toolbar center"> <div class="btn-group"> ';

			for(var tb in toolbar_buttons) if(toolbar_buttons.hasOwnProperty(tb)) {
				var button = toolbar_buttons[tb];
				if(button === null){
					toolbar += ' </div> <div class="btn-group"> ';
					continue;
				}
				
				if(typeof button == "string" && button in button_defaults) {
					button = button_defaults[button];
					button.name = toolbar_buttons[tb];
				} else if(typeof button == "object" && button.name in button_defaults) {
					button = $.extend(button_defaults[button.name] , button);
				}
				else continue;
				
				var className = "className" in button ? button.className : '';
				switch(button.name) {
					case 'font':
						toolbar += ' <a class="btn btn-sm '+className+' dropdown-toggle" data-toggle="dropdown" title="'+button.title+'"><i class="'+button.icon+'"></i><i class="icon-angle-down icon-on-right"></i></a> ';
						toolbar += ' <ul class="dropdown-menu dropdown-light">';
						for(var font in button.values)
							if(button.values.hasOwnProperty(font))
								toolbar += ' <li><a data-edit="fontName ' + button.values[font] +'" style="font-family:\''+ button.values[font]  +'\'">'+button.values[font]  + '</a></li> '
						toolbar += ' </ul>';
					break;

					case 'fontSize':
						toolbar += ' <a class="btn btn-sm '+className+' dropdown-toggle" data-toggle="dropdown" title="'+button.title+'"><i class="'+button.icon+'"></i>&nbsp;<i class="icon-angle-down icon-on-right"></i></a> ';
						toolbar += ' <ul class="dropdown-menu dropdown-light"> ';
						for(var size in button.values)
							if(button.values.hasOwnProperty(size))
								toolbar += ' <li><a data-edit="fontSize '+size+'"><font size="'+size+'">'+ button.values[size] +'</font></a></li> '
						toolbar += ' </ul> ';
					break;

					case 'createLink':
						toolbar += ' <div class="inline position-relative"> <a class="btn btn-sm '+className+' dropdown-toggle" data-toggle="dropdown" title="'+button.title+'"><i class="'+button.icon+'"></i></a> ';
						toolbar += ' <div class="dropdown-menu dropdown-caret pull-right">\
							<div class="input-group">\
								<input class="form-control" placeholder="'+button.placeholder+'" type="text" data-edit="'+button.name+'" />\
								<span class="input-group-btn">\
									<button class="btn btn-sm '+button.button_class+'" type="button">'+button.button_text+'</button>\
								</span>\
							</div>\
						</div> </div>';
					break;

					case 'insertImage':
						toolbar += ' <div class="inline position-relative"> <a class="btn btn-sm '+className+' dropdown-toggle" data-toggle="dropdown" title="'+button.title+'"><i class="'+button.icon+'"></i></a> ';
						toolbar += ' <div class="dropdown-menu dropdown-caret pull-right">\
							<div class="input-group">\
								<input class="form-control" placeholder="'+button.placeholder+'" type="text" data-edit="'+button.name+'" />\
								<span class="input-group-btn">\
									<button class="btn btn-sm '+button.button_insert_class+'" type="button">'+button.button_insert+'</button>\
								</span>\
							</div>';
							if( button.choose_file && 'FileReader' in window ) toolbar +=
							 '<div class="space-2"></div>\
							 <div class="center">\
								<button class="btn btn-sm '+button.button_class+' wysiwyg-choose-file" type="button">'+button.button_text+'</button>\
								<input type="file" data-edit="'+button.name+'" />\
							  </div>'
						toolbar += ' </div> </div>';
					break;

					case 'foreColor':
					case 'backColor':
						toolbar += ' <select class="hide wysiwyg_colorpicker" title="'+button.title+'"> ';
						for(var color in button.values)
							toolbar += ' <option value="'+button.values[color]+'">'+button.values[color]+'</option> ';
						toolbar += ' </select> ';
						toolbar += ' <input style="display:none;" disabled class="hide" type="text" data-edit="'+button.name+'" /> ';
					break;

					case 'viewSource':
						toolbar += ' <a class="btn btn-sm '+className+'" data-view="source" title="'+button.title+'"><i class="'+button.icon+'"></i></a> ';
					break;
					default:
						toolbar += ' <a class="btn btn-sm '+className+'" data-edit="'+button.name+'" title="'+button.title+'"><i class="'+button.icon+'"></i></a> ';
					break;
				}
			}
			toolbar += ' </div> </div> ';



			//if we have a function to decide where to put the toolbar, then call that
			if(options.toolbar_place) toolbar = options.toolbar_place.call(this, toolbar);
			//otherwise put it just before our DIV
			else toolbar = $(this).before(toolbar).prev();

			toolbar.find('a[title]').tooltip({animation:false, container:'body'});
			toolbar.find('.dropdown-menu input:not([type=file])').on(ace.click_event, function() {return false})
		    .on('change', function() {$(this).closest('.dropdown-menu').siblings('.dropdown-toggle').dropdown('toggle')})
			.on('keydown', function (e) {if(e.which == 27) {this.value='';$(this).change()}});
			toolbar.find('input[type=file]').prev().on(ace.click_event, function (e) { 
				$(this).next().click();
			});
			toolbar.find('.wysiwyg_colorpicker').each(function() {
				$(this).ace_colorpicker({pull_right:true}).change(function(){
					$(this).nextAll('input').eq(0).val(this.value).change();
				}).next().find('.btn-colorpicker').tooltip({title: this.title, animation:false, container:'body'})
			});
			
			var speech_input;
			if (options.speech_button && 'onwebkitspeechchange' in (speech_input = document.createElement('input'))) {
				var editorOffset = $(this).offset();
				toolbar.append(speech_input);
				$(speech_input).attr({type:'text', 'data-edit':'inserttext','x-webkit-speech':''}).addClass('wysiwyg-speech-input')
				.css({'position':'absolute'}).offset({top: editorOffset.top, left: editorOffset.left+$(this).innerWidth()-35});
			} else speech_input = null
			
			
			//view source
			var self = $(this);
			var view_source = false;
			toolbar.find('a[data-view=source]').on('click', function(e){
				e.preventDefault();
				
				if(!view_source) {
					$('<textarea />')
					.css({'width':self.outerWidth(), 'height':self.outerHeight()})
					.val(self.html())
					.insertAfter(self)
					self.hide();
					
					$(this).addClass('active');
				}
				else {
					var textarea = self.next();
					self.html(textarea.val()).show();
					textarea.remove();
					
					$(this).removeClass('active');
				}
				
				view_source = !view_source;
			});


			var $options = $.extend({}, { activeToolbarClass: 'active' , toolbarSelector : toolbar }, options.wysiwyg || {})
			$(this).wysiwyg( $options );
		});

		return this;
	}


})(window.jQuery);