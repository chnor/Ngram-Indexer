
n = Integer ARGF.argv[0]

files = []

a = (0..9).to_a
a << "_ADJ_"
a << "_ADP_"
a << "_ADV_"
a << "_CONJ_"
a << "_DET_"
a << "_NOUN_"
a << "_NUM_"
a << "_PRON_"
a << "_PRT_"
a << "_VERB_"
a << "_ADJ_"
a << "_ADJ_"
files += a.map {|a| "http://storage.googleapis.com/books/ngrams/books/googlebooks-eng-all-#{n}gram-20120701-#{a}.gz"}

x = ("a".."z").to_a
y = ["_"] + x
files += x.product(y).map {|x, y| "http://storage.googleapis.com/books/ngrams/books/googlebooks-eng-all-#{n}gram-20120701-#{x}#{y}.gz"}

for file in files
	puts file
end