
require 'open-uri'
require 'thread'
require 'zlib'
require 'set'

paths = ARGF.read.split.map &:strip

paths_done = open("paths_done").read.split.map(&:strip).to_set

Dir.chdir(File.dirname(__FILE__)) do
	IO.popen('java -cp "lib/*" org.apache.lucene.ngram.IndexNgramFeatures -index index_features', 'r+') do |indexer|
		
		retrieving = true
		shutting_down = false
		outputting = true

		downloading = false
		downloading_amount = 0
		downloading_size = 1
		indexing = false
		indexing_amount = 0
		indexing_size = 1

		message_queue = Queue.new

		begin

			indexer_output_thread = Thread.new do
				while line = indexer.gets do
					# message_queue << line
				end
			end

			output_thread = Thread.new do
				
				last_downloading_amount = 0
				last_indexing_amount = 0
				last_time = Time.now
				while outputting
					print "\r#{' ' * 120}\r"

					until message_queue.empty?
						puts message_queue.pop
					end

					elapsed = Time.now - last_time

					s_1 = ""
					if downloading
						amount = downloading_amount
						doc_size = downloading_size
						speed = (amount - last_downloading_amount).to_f / elapsed
						s_1 = "Downloaded #{amount / (2**20)} MB (#{(100.0 * amount / doc_size).round(2)}%) @ #{(speed/2**10).round(2)} KB/s"
						last_downloading_amount = amount
					else
						s_1 = "Downloader idle"
					end
					print s_1[0..60]
					print " " * [60 - s_1.size, 0].max

					s_2 = ""
					if indexing
						amount = indexing_amount
						doc_size = indexing_size
						speed = (amount - last_indexing_amount).to_f / elapsed
						s_2 = "Indexed #{amount / (2**20)} MB (#{(100.0 * amount / doc_size).round(2)}%) @ #{(speed/2**10).round(2)} KB/s"
						last_indexing_amount = amount
					else
						s_2 = "Indexer idle"
					end
					print s_2[0..60]

					last_time = Time.now
					sleep 0.1
				end

			end

			if paths.empty?

			else

				docs = SizedQueue.new 4
				
				producer = Thread.new do
					begin

						for path in paths
							break unless retrieving
							next if paths_done.include? path

							done = false
							until done
								begin
									if path.end_with? ".gz"
										doc = nil
										message_queue << "Retrieving #{path}"
										doc_size = 0

										# last_amount = 0
										# last_time = Time.now
										# doc = open path
										downloading = true
										doc = open(path,
										:content_length_proc => lambda { |t|
											doc_size = t
											downloading_size = t
										},
										:progress_proc => lambda { |amount|
											# elapsed = Time.now - last_time
											# if elapsed > 0.1
												downloading_amount = amount
												# speed = (amount - last_amount).to_f / elapsed
												# print "\r#{' ' * 50}\r"
												# print "Completed #{amount / (2**20)} MB (#{(100.0 * amount / doc_size).round(2)}%) \t@ #{speed.round(2)} KB/s"
												# last_time = Time.now
												# last_amount = amount
											# end
										})
										downloading = false
										# puts
										gz_doc = Zlib::GzipReader.new doc
										# Make certain the variable stays around and
										# isn't eaten by the GC or something like that
										# doc_size = doc.size

										# Monkey patch on utility variables
										gz_doc.instance_eval do
											class << self
												attr_reader :orig_path
												attr_reader :doc_size
											end
											@orig_path = path
											@doc_size = doc_size
										end

										docs << gz_doc
									else
										docs << (open path)
									end
									done = true
								rescue Exception => e
									unless shutting_down
										# Failed
										# Assume that this is due to network problems
										message_queue << " "
										message_queue << "Failed to open document"
										message_queue << e.message
										# for t in 60.downto(1)
										# 	print "\rRetrying in #{t} seconds  "
										# 	sleep 1
										# end
										# puts "\r#{' ' * 30}"
										sleep 60
										message_queue << "Retrying"
										# Retry
									end
								ensure
									message_queue << "Finished retrieval process"
								end
							end
						end

					retrieving = false # Atomicity is not an issue here
					end
				end

				consumer = Thread.new do
					while retrieving or not docs.empty?
						# puts "Waiting for documents..."
						doc = docs.pop
						next if doc == nil
						message_queue << "Indexing contents for #{doc.orig_path}"
						indexing_size = Float::INFINITY
						indexing = true
						for line in doc
							indexer.puts line
							
							indexing_amount = doc.pos
						end
						indexing = false
						message_queue << "Finished processing for #{doc.orig_path}"
						doc.close

						# File appends are supposed to be atomic at this
						# size in POSIX. See:
						# http://stackoverflow.com/questions/1154446/is-file-append-atomic-in-unix
						`echo '#{doc.orig_path}' >> paths_done`

					end
				end
			end

			while retrieving
				c = STDIN.gets.chomp
				if c == "q"
					message_queue << "Quitting..."
					retrieving = false
					docs.clear
					docs.push nil
					message_queue << "Waiting for indexer to finish current set..."
					shutting_down = true
					producer.kill
					downloading = false
				end
			end
		ensure
			message_queue << "Exiting..."
			retrieving = false
			docs.clear
			docs.push nil

			consumer.join if consumer
			indexer.close_write
			outputting = false
			indexer_output_thread.join
			output_thread.join

			puts
			puts "Exited"
			puts "Index should not be corrupted"
		end
	end
end
