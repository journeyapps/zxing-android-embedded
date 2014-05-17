require 'fileutils'

PREFIX = 'zxing'

`mkdir -p assets/#{PREFIX}`
`mkdir -p src`
`mkdir -p res`
`mkdir -p android-core-src`
`cp ../../zxing/android/AndroidManifest.xml ./`
`cp -r ../../zxing/android/assets/* ./assets/#{PREFIX}/`
`cp -r ../../zxing/android/src/* ./src/`
`cp -r ../../zxing/android/res/* ./res/`
`cp -r ../../zxing/android-core/src/main/java/* ./android-core-src/`


def process(files)
  files.each do |file|
    text = File.read(file)
    processed = yield text
    File.open(file, 'w') { |file| file.puts processed }
  end
end

process Dir['res/**/strings.xml'] do |text|
  text.gsub(/<string name="(?<name>.*?)">(?<value>.*?)<\/string>/, "<string name=\"#{PREFIX}_\\k<name>\">\\k<value></string>")
end

process %w(res/values/arrays.xml) do |text|
  text.gsub(/<string-array name="(?<name>.*?)">/, "<string-array name=\"#{PREFIX}_\\k<name>\">")
end

process %w(res/values/colors.xml) do |text|
  text.gsub(/<color name="(?<name>.*?)">(?<value>.*?)<\/color>/, "<color name=\"#{PREFIX}_\\k<name>\">\\k<value></color>")
end

process %w(res/values/dimens.xml) do |text|
  text.gsub(/<dimen name="(?<name>.*?)">(?<value>.*?)<\/dimen>/, "<dimen name=\"#{PREFIX}_\\k<name>\">\\k<value></dimen>")
end

process %w(res/values/ids.xml) do |text|
  text.gsub(/<item type="id" name="(?<name>.*?)"\/>/, "<item type=\"id\" name=\"#{PREFIX}_\\k<name>\"/>")
end

process %w(res/values/styles.xml res/values/themes.xml) do |text|
  text.gsub(/<style name="/, "<style name=\"#{PREFIX}_")
end

process %w(res/xml/preferences.xml) do |text|
  text.gsub(/android:key="/, "android:key=\"#{PREFIX}_")
end

# @color, @drawable, @+id, @string, @layout, etc
process Dir['res/**/*.xml'] + ['AndroidManifest.xml'] do |text|
  text.gsub(/(@\+?\w+?)\//, "\\1/#{PREFIX}_")
end

process %w(AndroidManifest.xml) do |text|
  text.gsub!(/android:minSdkVersion="\d+"/, 'android:minSdkVersion="7"')
  text.gsub!(/android:versionName=".+?"\s*/, '')
  text.gsub!(/android:versionCode=".+?"\s*/, '')
  text.gsub!(/android:installLocation=".+?"\s*/, '')

  # Remove <intent-filter>s
  # text.gsub!(/<activity(.+?[^\/])>(.+?)<\/activity>/m, '<activity\1 />')
  text.gsub!(/<intent-filter>(.+?)<\/intent-filter>/m, '')

  text
end

# Rename resource files
Dir['res/**/*.*'].each do |file|
  unless file.include? "/#{PREFIX}_"
    new_name = file.gsub(/([\w_]+\.[\w_]+)/, "#{PREFIX}_\\1")
    FileUtils.rm new_name if File.exists? new_name
    FileUtils.mv file, new_name
  end
end

process Dir['**/*.java'] do |text|
  # Replace resource ids
  text.gsub!(/([^\.])R\.(string|layout|id|xml|menu|color|drawable|raw)\./, "\\1R.\\2.#{PREFIX}_")

  # Very nasty hack to convert (some) switch statements into if statements
  text.gsub!(/    switch \(.+?\) \{(.+?)\n    \}/m) do |body|
    if body.include? 'case R.id.'
      body.gsub!(/switch \((.+?)\) \{/, 'int switchValue = \1;')
      body.gsub!(/(  break;\s+?)?  case (.+?)\:/, '} else if(switchValue == \2) {')
      body.gsub!(/(  break;\s+?)?  default\:/, '} else {')
      body.sub!(/^(\s+)\} else if/, '\1if')
      body.gsub!(/\n\s+break;/, '')
      body
    else
      body
    end
  end

  # Replace asset references
  text.gsub!('file:///android_asset/', "file:///android_asset/#{PREFIX}/")

  text
end

# Preference names
process Dir['**/PreferencesActivity.java'] do |text|
  text.gsub(' = "preferences_', ' = "#{PREFIX}_preferences_')
end
