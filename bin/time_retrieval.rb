
require 'thread'

words = open("/usr/share/dict/words").read.split("\n").map &:strip

dryrun_words = words.sample 100
test_words = words.sample 1000

Dir.chdir(File.dirname(__FILE__)) do
	IO.popen('java -cp "lib/*" org.apache.lucene.ngram.ExtractNgrams -index index_3', 'r+') do |search|
		
		t_out = Thread.new do
			while line = search.gets do
				# puts line
			end
		end
		
		for word in dryrun_words
			search.puts word
		end

		# Let the output catch up
		sleep 5

		t_0 = Time.now
		for word in test_words
			search.puts word
		end
		search.close_write
		t_out.join
		puts "Result: #{(Time.now - t_0) / test_words.size} s"

	end
end
