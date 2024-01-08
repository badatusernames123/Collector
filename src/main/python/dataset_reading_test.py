import pickle
import gzip

def open_dataset():
    # Open the dataset from the compressed pickle file
    with gzip.open('dataset.pkl.gz', 'rb') as file:
        dataset = pickle.load(file)
    return dataset

dataset = open_dataset()
print(dataset[0][2])
for example in dataset:
    print(example[2])