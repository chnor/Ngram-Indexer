
Dir.chdir("../lucene-4.10.3_src/google_ngram/") do
	
	puts "Compiling..."
	puts
	IO.popen("ant -lib /usr/local/Cellar/ivy/2.4.0/libexec/") do |output|
		while line = output.gets do
			puts line
		end
	end

end

`cp ../lucene-4.10.3_src/build/ngram/lucene-ngram-*-SNAPSHOT.jar ./lib/`
