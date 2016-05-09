/**
Image editable input.
**/
(function ($) {
    "use strict";
    
    var Image = function (options) {
        this.init('image', options, Image.defaults);

		if('on_error' in options.image) {
			this.on_error = options.image['on_error'];
			delete options.image['on_error']
		}
		if('on_success' in options.image) {
			this.on_success = options.image['on_success'];
			delete options.image['on_success']
		}
		if('max_size' in options.image) {
			this.max_size = options.image['max_size'];
			delete options.image['max_size']
		}

		this.initImage(options, Image.defaults);
    };

    //inherit from Abstract input
    $.fn.editableutils.inherit(Image, $.fn.editabletypes.abstractinput);

    $.extend(Image.prototype, {
		initImage: function(options, defaults) {
          this.options.image = $.extend({}, defaults.image, options.image);
		  this.name = this.options.image.name || 'editable-image-input';
        },

        /**
        Renders input from tpl

        @method render() 
        **/        
        render: function() {
			var self = this;
			this.$input = this.$tpl.find('input[type=hidden]:eq(0)');
			this.$file = this.$tpl.find('input[type=file]:eq(0)');

			this.$file.attr({'name':this.name});
			this.$input.attr({'name':this.name+'-hidden'});

			
			this.options.image.before_change = this.options.image.before_change || function(files, dropped) {
				var file = files[0];
				if(typeof file === "string") {
					//files is just a file name here (in browsers that don't support FileReader API)
					if(! (/\.(jpe?g|png|gif)$/i).test(file) ) {
						if(self.on_error) self.on_error(1);
						return false;
					}
				}
				else {//file is a File object
					var type = $.trim(file.type);
					if( ( type.length > 0 && ! (/^image\/(jpe?g|png|gif)$/i).test(type) )
							|| ( type.length == 0 && ! (/\.(jpe?g|png|gif)$/i).test(file.name) )//for android default browser!
						)
					{
						if(self.on_error) self.on_error(1);
						return false;
					}
					if( self.max_size && file.size > self.max_size ) {
						if(self.on_error) self.on_error(2);
						return false;
					}
				}

				if(self.on_success) self.on_success();
				return true;
			}
			this.options.image.before_remove = this.options.image.before_remove || function() {
				self.$input.val(null);
				return true;
			}

			this.$file.ace_file_input(this.options.image).on('change', function(){
				var $rand = (self.$file.val() || self.$file.data('ace_input_files')) ? Math.random() + "" + (new Date()).getTime() : null;
				self.$input.val($rand)//set a random value, so that selected file is uploaded each time, even if it's the same file, because inline editable plugin does not update if the value is not changed!
			}).closest('.ace-file-input').css({'width':'150px'}).closest('.editable-input').addClass('editable-image');
		}

    });

	
    Image.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
        tpl: '<span><input type="hidden" /></span><span><input type="file" /></span>',
        inputclass: '',
		image:
		{
			style: 'well',
			btn_choose: 'Change Image',
			btn_change: null,
			no_icon: 'icon-picture',
			thumbnail: 'large'
		}
    });

    $.fn.editabletypes.image = Image;

}(window.jQuery));










//Wysiwyg
(function ($) {
    "use strict";
    
    var Wysiwyg = function (options) {
        this.init('wysiwyg', options, Wysiwyg.defaults);
        
        //extend wysiwyg manually as $.extend not recursive 
        this.options.wysiwyg = $.extend({}, Wysiwyg.defaults.wysiwyg, options.wysiwyg);
    };

    $.fn.editableutils.inherit(Wysiwyg, $.fn.editabletypes.abstractinput);

    $.extend(Wysiwyg.prototype, {
        render: function () {
			this.$editor = this.$input.nextAll('.wysiwyg-editor:eq(0)');
			
			this.$tpl.parent().find('.wysiwyg-editor').show().ace_wysiwyg(
			 {
				toolbar:
				[
				'bold',
				'italic',
				'strikethrough',
				'underline',
				null,
				'foreColor',
				null,
				'insertImage'
				]
			  }
			)
			.prev().addClass('wysiwyg-style2')
			.closest('.editable-input').addClass('editable-wysiwyg')
			.closest('.editable-container').css({'display':'block'});//if display is inline-block, putting large images inside the editor will expand it out of bounding box!

			if(this.options.wysiwyg && this.options.wysiwyg.css) 
				this.$tpl.closest('.editable-wysiwyg').css(this.options.wysiwyg.css);
        },


        value2html: function(value, element) {
            $(element).html(value);
			return false;
        },

        html2value: function(html) {
			return html;
        },

        value2input: function(value) {
			this.$editor.html(value);
        },
		input2value: function() { 
			return this.$editor.html();
        },

        activate: function() {
           //this.$editor.focus().get(0).setSelectionRange(200,200);
        }
    });
	


    Wysiwyg.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
		tpl: '<input type="hidden" /><div class="wysiwyg-editor"></div>',
        inputclass: 'editable-wysiwyg',
        wysiwyg: {
            
        }
    });

    $.fn.editabletypes.wysiwyg = Wysiwyg;

}(window.jQuery));








/**
Spinner editable input.
**/
(function ($) {
    "use strict";
    
    var Spinner = function (options) {
        this.init('spinner', options, Spinner.defaults);
		this.initSpinner(options, Spinner.defaults);
    };

    //inherit from Abstract input
    $.fn.editableutils.inherit(Spinner, $.fn.editabletypes.abstractinput);

    $.extend(Spinner.prototype, {
		initSpinner: function(options, defaults) {
            this.options.spinner = $.extend({}, defaults.spinner, options.spinner);
        },

        /**
        Renders input from tpl

        @method render() 
        **/        
        render: function() {
		},
       
        /**
        Activates input: sets focus on the first field.
        
        @method activate() 
       **/        
       activate: function() {
            if(this.$input.is(':visible')) {
				this.$input.focus();
				$.fn.editableutils.setCursorPosition(this.$input.get(0), this.$input.val().length);
				
				var val = parseInt(this.$input.val());
				var options = $.extend({value:val}, this.options.spinner);
				this.$input.ace_spinner(options);
            }
       },
       
       /**
        Attaches handler to submit form in case of 'showbuttons=false' mode
        
        @method autosubmit() 
       **/       
       autosubmit: function() {
           this.$input.keydown(function (e) {
                if (e.which === 13) {
                    $(this).closest('form').submit();
                }
           });
       }       
    });

    Spinner.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
        tpl: '<input type="text" />',
        inputclass: '',
		spinner:{
			min:0,
			max:100,
			step:1,
			icon_up:'icon-plus',
			icon_down:'icon-minus',
			btn_up_class:'btn-success',
			btn_down_class:'btn-danger'
        }
    });

    $.fn.editabletypes.spinner = Spinner;

}(window.jQuery));








/**
Slider editable input.
**/
(function ($) {
    "use strict";
    
    var Slider = function (options) {
        this.init('slider', options, Slider.defaults);
		this.initSlider(options, Slider.defaults);
    };

    //inherit from Abstract input
    $.fn.editableutils.inherit(Slider, $.fn.editabletypes.abstractinput);

    $.extend(Slider.prototype, {
		initSlider: function(options, defaults) {
            this.options.slider = $.extend({}, defaults.slider, options.slider);
        },

        /**
        Renders input from tpl

        @method render() 
        **/        
        render: function() {
		},
        /**
        Activates input: sets focus on the first field.
        
        @method activate() 
       **/
       activate: function() {
            if(this.$input.is(':visible')) {
				this.$input.focus();
				$.fn.editableutils.setCursorPosition(this.$input.get(0), this.$input.val().length);

					var self = this;
					var val = parseInt(this.$input.val());
					var width = this.options.slider.width || 200;
					var options = $.extend(this.options.slider , {
						value:val,
						slide: function( event, ui ) {
							var val = parseInt(ui.value);
							self.$input.val(val);
							
							if(ui.handle.firstChild == null) {/* no tooltips attached to it */
								$(ui.handle).append("<div class='tooltip top in' style='display:none;top:-38px;left:-5px;'><div class='tooltip-arrow'></div><div class='tooltip-inner'></div></div>");
							}
							$(ui.handle.firstChild).show().children().eq(1).text(val);
						}
					});

					this.$input.parent().addClass('editable-slider').css('width', width+'px').slider(options);
            }
       },
	   
	   value2html: function(value, element) {
       },

       /**
        Attaches handler to submit form in case of 'showbuttons=false' mode
        
        @method autosubmit() 
       **/       
       autosubmit: function() {
           this.$input.keydown(function (e) {
                if (e.which === 13) {
                    $(this).closest('form').submit();
                }
           });
       }       
    });

    Slider.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
        tpl: '<input type="text" /><span class="inline ui-slider-green"><span class="slider-display"></span></span>',
        inputclass: '',
		slider:{
			min:1,
			max:100,
			step:1,
			range: "min"
        }
    });

    $.fn.editabletypes.slider = Slider;

}(window.jQuery));

