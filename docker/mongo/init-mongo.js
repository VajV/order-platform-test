db = db.getSiblingDB('product_db');

db.createUser({
  user: 'product_user',
  pwd: 'product_pass',
  roles: [{ role: 'readWrite', db: 'product_db' }]
});

db.createCollection('products');
db.createCollection('categories');

db.products.createIndex({ "productId": 1 }, { unique: true });
db.products.createIndex({ "name": "text", "description": "text" });

db.categories.insertMany([
  { name: "Electronics", description: "Electronic devices" },
  { name: "Clothing", description: "Apparel" },
  { name: "Books", description: "Books and magazines" }
]);

print('MongoDB initialization completed');
