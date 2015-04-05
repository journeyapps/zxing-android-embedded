require 'fileutils'

`rm -rf src-orig/* res-orig/*`
`cp -r ../zxing-android-complete/res/* res-orig/`

def remove_all prefix, globs
  globs.each do |glob|
    Dir[prefix + glob].each do |file|
      if File.directory?(file)
        FileUtils.rm_r file
      else
        FileUtils.rm file
      end
    end
  end
end

def process(files)
  files.each do |file|
    text = File.read(file)
    processed = yield text
    File.open(file, 'w') { |file| file.puts processed }
  end
end


# Remove strings we don't use

remove_strings = <<-LINES
zxing_history_.*?
zxing_preferences_.*?
zxing_result_.*?
zxing_contents_.+?
zxing_app_picker_name
zxing_bookmark_picker_name
zxing_button_add_calendar
zxing_button_add_contact
zxing_button_book_search
zxing_button_cancel
zxing_button_custom_product_search
zxing_button_dial
zxing_button_email
zxing_button_get_directions
zxing_button_mms
zxing_button_open_browser
zxing_button_product_search
zxing_button_search_book_contents
zxing_button_share_app
zxing_button_share_bookmark
zxing_button_share_by_email
zxing_button_share_by_sms
zxing_button_share_clipboard
zxing_button_share_contact
zxing_button_show_map
zxing_button_sms
zxing_button_web_search
zxing_button_wifi
zxing_menu_history
zxing_menu_settings
zxing_menu_encode_mecard
zxing_menu_encode_vcard
zxing_menu_help
zxing_menu_share
zxing_msg_bulk_mode_scanned
zxing_msg_default_format
zxing_msg_default_meta
zxing_msg_default_mms_subject
zxing_msg_default_time
zxing_msg_default_type
zxing_msg_error
zxing_msg_google_books
zxing_msg_google_product
zxing_msg_intent_failed
zxing_msg_invalid_value
zxing_msg_redirect
zxing_msg_sbc_book_not_searchable
zxing_msg_sbc_failed
zxing_msg_sbc_no_page_returned
zxing_msg_sbc_page
zxing_msg_sbc_results
zxing_msg_sbc_searching_book
zxing_msg_sbc_snippet_unavailable
zxing_msg_share_explanation
zxing_msg_share_text
zxing_msg_sure
zxing_msg_encode_contents_failed
zxing_msg_unmount_usb
zxing_sbc_name
zxing_wifi_changing_network
LINES
remove_strings = remove_strings.split

process Dir['res-orig/**/zxing_strings.xml'] do |text|
  remove_strings.each do |rm|
    text.gsub!(/\s+<string name="#{rm}">.*?<\/string>/, '')
  end

  text
end

# Remove unused resource files
remove_all 'res-orig/', %w(drawable drawable-*)
remove_all 'res-orig/', %w(layout layout-ldpi layout-land menu xml)
remove_all 'res-orig/values/', %w(zxing_arrays.xml zxing_dimens.xml zxing_styles.xml zxing_ids.xml zxing_themes.xml zxing_colors.xml)
