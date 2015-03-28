require 'fileutils'

`rm -rf src-orig/* res-orig/*`
`cp -r ../zxing-android-complete/android-core-src/* src-orig/`
`cp -r ../zxing-android-complete/src/* src-orig/`
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

# Remove unsused source files
orig_prefix = 'src-orig/com/google/zxing/client/android/'

# We have custom versions of each of these files - remove the original ones and log the diff
FileUtils.rm_f 'source.patch'
Dir['src/com/google/zxing/client/android/**/*.java'].each do |our_file|
  orig_file = our_file.gsub(/src/, 'src-orig')
  if File.exists? orig_file
    `git diff --no-index #{orig_file} #{our_file} >> source.patch`
    FileUtils.rm orig_file
  end
end


remove_all orig_prefix, %w(HttpHelper.java HelpActivity.java ScanFromWebPageManager.java Contents.java IntentSource.java LocaleManager.java PreferencesFragment.java book history result share wifi clipboard encode)



# Remove strings we don't use

remove_strings = <<-LINES
zxing_history_.*?
zxing_preferences_.*?
zxing_result_.*?
zxing_app_picker_name
zxing_bookmark_picker_name
zxing_button_add_calendar
zxing_button_add_contact
zxing_button_book_search
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
zxing_msg_default_mms_subject
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

# Remove unused color value
process %w(res-orig/values/zxing_colors.xml) do |text|
  text.gsub!(/\s+<color name="zxing_encode_view">.*?<\/color>/, '')
  text
end

# Remove title and summary from preferences, since we don't use them,
# and we don't have the string resources anymore.
process %w(res-orig/xml/zxing_preferences.xml) do |text|
  text.gsub! /\s+android:title=".+?"/, ''
  text.gsub! /\s+android:summary=".+?"/, ''
  text.gsub! /\s+android:entries=".+?"/, ''
  text
end

# We have custom versions of each of these files - remove the original ones and log the diff
FileUtils.rm_f 'res.patch'
Dir['res/**/*.xml'].each do |our_file|
  orig_file = our_file.gsub(/res/, 'res-orig')
  if File.exists? orig_file
    `git diff --no-index #{orig_file} #{our_file} >> res.patch`
    FileUtils.rm orig_file
  end
end


# Remove unused resource files
remove_all 'res-orig/', %w(drawable drawable-*)
remove_all 'res-orig/', %w(layout layout-ldpi layout-land menu)
