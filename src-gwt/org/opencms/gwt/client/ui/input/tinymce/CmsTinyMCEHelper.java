/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH & Co. KG (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.gwt.client.ui.input.tinymce;

import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.editors.CmsTinyMceToolbarHelper;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * Helper class for dealing wth TinyMCE widgets.<p>
 */
public final class CmsTinyMCEHelper {

    /**
     * Hidden default constructor.<p>
     */
    private CmsTinyMCEHelper() {

        // nothing here
    }

    /**
     * Generate options for TinyMCE.<p>
     *
     * @param configuration the widget configuration generated by the server
     *
     * @return the TinyMCE configuration
     */
    public static JavaScriptObject generateOptionsForTiny(String configuration) {

        return generateOptionsForTiny(configuration, getCodeMirrorPath());
    }

    /**
    * Generates the tinyMCE editor options according to the configuration.<p>
    *
    * @param configuration the widget configuration
    * @param codeMirrorPath the path to the CodeMirror editor
    *
    * @return the tinyMCE options
    */
    public static native JavaScriptObject generateOptionsForTiny(String configuration, String codeMirrorPath)/*-{

		var options = null;
		try {
			var config = @org.opencms.gwt.client.util.CmsDomUtil::parseJSON(Ljava/lang/String;)(configuration);
			options = {
				entity_encoding : 'named',
				entities : '160,nbsp',
				// the browser call back function is defined in /system/workplace/editors/tinymce/opencms_plugin.js
				file_picker_callback : $wnd.cmsTinyMceFileBrowser
			};
			if (config.downloadGalleryConfig) {
				options.downloadGalleryConfig = config.downloadGalleryConfig;
			}

			if (config.imageGalleryConfig) {
				options.imageGalleryConfig = config.imageGalleryConfig;
			}

			if (config.language) {
                var languageMap = { "it": "it_IT", "cs": "cs_CZ", "ru": "ru_RU", "zh": "zh_CN"};
                var translatedLanguage = languageMap[config.language];
                if (translatedLanguage) {
                    options.language = translatedLanguage;
                } else {
				    options.language = config.language;
				}
			}
			if (config.content_css) {
				options.content_css = config.content_css;
			}
			options.importcss_append = true;
			if (config.importCss) {
				options.importcss_selector_filter = ""; // always matches
			} else {
				options.importcss_selector_filter = new $wnd.RegExp("$.^"); // never matches
			}
			if (config.height) {
				var height = parseInt(config.height);
				if (height != NaN) {
					options.editorHeight = height;
				}
			}
			if (config.block_formats) {
				options.block_formats = config.block_formats;
			}
			if (config.style_formats) {
				var temp = null;
				try {
					temp = eval('(' + config.style_formats + ')');
				} catch (error) {
					$wnd.alert("Could not parse WYSIWYG editor options: "
							+ error);
				}
				if (typeof temp != 'undefined' && temp != null) {
					options.style_formats = temp;
				}
			}
			if (config.cmsGalleryEnhancedOptions) {
				options.cmsGalleryEnhancedOptions = config.cmsGalleryEnhancedOptions;
			}
			if (config.cmsGalleryUseThickbox) {
				options.cmsGalleryUseThickbox = config.cmsGalleryUseThickbox;
			}
			options.plugins = "anchor charmap importcss autolink lists pagebreak table save hr codemirror image link emoticons insertdatetime preview media searchreplace print paste directionality fullscreen noneditable visualchars nonbreaking template wordcount advlist spellchecker -opencms";
			options.preview_styles="font-family font-size font-weight font-style text-decoration text-transform border border-radius outline text-shadow";
			if (config.fullpage) {
				options.plugins += " fullpage";
			}
			// add codemirror source view plugin configuration
			options.codemirror = {
				indentOnInit : true, // whether or not to indent code on init.
				path : @org.opencms.gwt.client.ui.input.tinymce.CmsTinyMCEHelper::getCodeMirrorPath()(), // path to CodeMirror distribution
				config : { // CodeMirror config object
					lineNumbers : true
				}
			};
			if (config.allowscripts) {
				options.valid_elements = "*[*]";
				options.allow_script_urls = true;
			}
			if (config.link_default_protocol) {
				options.link_default_protocol = config.link_default_protocol;
			}
			if (config.toolbar_items) {
				toolbarGroup = @org.opencms.gwt.client.ui.input.tinymce.CmsTinyMCEHelper::createToolbar(Lcom/google/gwt/core/client/JavaScriptObject;)(config.toolbar_items);
				toolbarGroup += " | spellchecker";
				options.toolbar1 = toolbarGroup;
				var contextmenu = @org.opencms.gwt.client.ui.input.tinymce.CmsTinyMCEHelper::createContextMenu(Lcom/google/gwt/core/client/JavaScriptObject;)(config.toolbar_items);

				if (config.spellcheck_url) {
					options.spellchecker_language = config.spellcheck_language;
					options.spellchecker_languages = config.spellcheck_language;
					options.spellchecker_rpc_url = config.spellcheck_url;
					options.spellchecker_callback = function(name, text,
							onSuccess, onFailure) {
						$wnd.tinymce.util.JSONRequest.sendRPC({
							url : config.spellcheck_url,
							method : "spellcheck",
							params : {
								lang : this.getLanguage(),
								words : text.match(this.getWordCharPattern())
							},
							success : function(result) {
								onSuccess({
									words : result
								});
							},
							error : function(error, xhr) {
								onFailure("Spellcheck error:" + xhr.status);
							}
						});
					};
					contextmenu += " spellchecker"
				}
				if (contextmenu != "") {
					options.contextmenu = contextmenu;
				}
				if (config.pasteOptions) {
					options.paste_as_text = config.pasteOptions.paste_text_sticky_default ? true
							: false;
				}
				if (config.directOptions) {
				    for (var key in config.directOptions) {
				        options[key] = config.directOptions[key];
				    }
				}
			}

		} catch (e) {
			// nothing to do
		}

		return options;
    }-*/;

    /**
     * Returns the code mirror path.<p>
     *
     * @return the code mirror resource path
     */
    public static String getCodeMirrorPath() {

        return CmsStringUtil.joinPaths(CmsCoreProvider.get().getWorkplaceResourcesPrefix(), "editors/codemirror/dist/");
    }

    /**
     * Creates the TinyMCE toolbar config string from a Javascript config object.<p>
     *
     * @param jso a Javascript array of toolbar items
     *
     * @return the TinyMCE toolbar config string
     */
    protected static String createContextMenu(JavaScriptObject jso) {

        JsArray<?> jsItemArray = jso.<JsArray<?>> cast();
        List<String> jsItemList = new ArrayList<String>();
        for (int i = 0; i < jsItemArray.length(); i++) {
            jsItemList.add(jsItemArray.get(i).toString());
        }
        return CmsTinyMceToolbarHelper.getContextMenuEntries(jsItemList);
    }

    /**
     * Creates the TinyMCE toolbar config string from a Javascript config object.<p>
     *
     * @param jso a Javascript array of toolbar items
     *
     * @return the TinyMCE toolbar config string
     */
    protected static String createToolbar(JavaScriptObject jso) {

        JsArray<?> jsItemArray = jso.<JsArray<?>> cast();
        List<String> jsItemList = new ArrayList<String>();
        for (int i = 0; i < jsItemArray.length(); i++) {
            jsItemList.add(jsItemArray.get(i).toString());
        }
        return CmsTinyMceToolbarHelper.createTinyMceToolbarStringFromGenericToolbarItems(jsItemList);
    }

}
