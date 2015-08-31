
index = ARGV[0]

Dir.chdir(File.dirname(__FILE__)) do

	IO.popen(%{java -cp "lib/*" org.apache.lucene.ngram.ExtractNgramFeatures -index #{index}}, 'r+') do |tagger|
		t_out = Thread.new do
			# i = 0
			# STDERR.puts "Retrieving timelines"
			# last_p = 0
			# while line = tagger.gets do
			for line in tagger
				# i += 1
				# p = (100 * i.to_f / STDIN.size).round(1)
				# STDERR.print "#{' ' * 20}\r#{p} % Done" unless p == last_p
				# last_p = p
				puts line
			end
			STDERR.puts
		end
		for query in STDIN
			# STDERR.puts "'#{query.strip}'"
			tagger.puts query.strip
		end
		tagger.close_write
		t_out.join
	end
end