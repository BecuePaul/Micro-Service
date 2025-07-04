document.addEventListener('DOMContentLoaded', () => {
    const API_URL = 'http://localhost:8080';
    const AUTH_TOKEN = btoa('admin:password');

    const commonHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Basic ${AUTH_TOKEN}`
    };

    // --- DOM Elements ---
    const customerForm = document.getElementById('form-create-customer');
    const productForm = document.getElementById('form-create-product');
    const orderForm = document.getElementById('form-create-order');
    const refreshCustomersBtn = document.getElementById('refresh-customers');
    const refreshProductsBtn = document.getElementById('refresh-products');
    const refreshOrdersBtn = document.getElementById('refresh-orders');
    const customersTable = document.querySelector('#customers-table tbody');
    const productsTable = document.querySelector('#products-table tbody');
    const ordersTable = document.querySelector('#orders-table tbody');
    const orderCustomerSelect = document.getElementById('order-customer-select');
    const orderItemsContainer = document.getElementById('order-items-container');
    const addOrderItemBtn = document.getElementById('add-order-item-btn');
    const statusLog = document.getElementById('status-log');

    // --- Data Cache ---
    let availableCustomers = [];
    let availableProducts = [];

    // --- UI Functions ---
    function showStatus(message, isError = false) {
        statusLog.textContent = message;
        statusLog.style.backgroundColor = isError ? '#e74c3c' : '#2ecc71';
        statusLog.classList.add('visible');
        setTimeout(() => statusLog.classList.remove('visible'), 3000);
    }

    // --- Data Fetching ---
    async function fetchData(endpoint) {
        try {
            const response = await fetch(`${API_URL}/${endpoint}`, { headers: { 'Authorization': `Basic ${AUTH_TOKEN}` } });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error(`Failed to fetch ${endpoint}:`, error);
            showStatus(`Error fetching ${endpoint}: ${error.message}`, true);
            return [];
        }
    }

    // --- Table & Select Builders ---
    const customerRowBuilder = (c) => `<td>${c.id}</td><td>${c.firstName}</td><td>${c.lastName}</td><td>${c.email}</td>`;
    const productRowBuilder = (p) => `<td>${p.id}</td><td>${p.name}</td><td>${p.description}</td><td>${p.price.toFixed(2)}€</td><td>${p.stock}</td>`;
    const orderRowBuilder = (o) => {
        const productsHtml = o.orderItems.map(item => `<li>${item.quantity} x (Prod ID: ${item.productId})</li>`).join('');
        return `<td>${o.id}</td><td>${o.customerId}</td><td>${new Date(o.orderDate).toLocaleString()}</td><td><ul>${productsHtml}</ul></td>`;
    };

    function populateTable(table, data, builder) {
        table.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = builder(item);
            table.appendChild(tr);
        });
    }

    // --- Order Form Specific Functions ---
    function addOrderItemRow() {
        const itemRow = document.createElement('div');
        itemRow.className = 'order-item-row';

        const productSelect = document.createElement('select');
        productSelect.className = 'order-product-select';
        availableProducts.forEach(p => {
            const option = document.createElement('option');
            option.value = p.id;
            option.textContent = `${p.name} (Stock: ${p.stock})`;
            productSelect.appendChild(option);
        });

        const quantityInput = document.createElement('input');
        quantityInput.type = 'number';
        quantityInput.className = 'order-quantity-input';
        quantityInput.value = '1';
        quantityInput.min = '1';

        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'btn-remove';
        removeBtn.textContent = 'Remove';
        removeBtn.onclick = () => itemRow.remove();

        itemRow.append(productSelect, quantityInput, removeBtn);
        orderItemsContainer.appendChild(itemRow);
    }

    // --- Refresh/Update Functions ---
    async function refreshCustomers() {
        availableCustomers = await fetchData('customers');
        populateTable(customersTable, availableCustomers, customerRowBuilder);
        orderCustomerSelect.innerHTML = '';
        availableCustomers.forEach(c => {
            const option = document.createElement('option');
            option.value = c.id;
            option.textContent = `${c.firstName} ${c.lastName} (ID: ${c.id})`;
            orderCustomerSelect.appendChild(option);
        });
        showStatus('Customers list updated.');
    }

    async function refreshProducts() {
        availableProducts = await fetchData('products');
        populateTable(productsTable, availableProducts, productRowBuilder);
        // Refresh existing order item rows with new product data if needed
        document.querySelectorAll('.order-product-select').forEach(select => {
            const selectedValue = select.value;
            select.innerHTML = '';
            availableProducts.forEach(p => {
                const option = document.createElement('option');
                option.value = p.id;
                option.textContent = `${p.name} (Stock: ${p.stock})`;
                select.appendChild(option);
            });
            select.value = selectedValue;
        });
        showStatus('Products list updated.');
    }

    async function refreshOrders() {
        const orders = await fetchData('orders');
        populateTable(ordersTable, orders, orderRowBuilder);
        showStatus('Orders list updated.');
    }

    // --- Event Listeners ---
    refreshCustomersBtn.addEventListener('click', refreshCustomers);
    refreshProductsBtn.addEventListener('click', refreshProducts);
    refreshOrdersBtn.addEventListener('click', refreshOrders);
    addOrderItemBtn.addEventListener('click', addOrderItemRow);

    customerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const body = JSON.stringify({
            firstName: document.getElementById('customer-firstname').value,
            lastName: document.getElementById('customer-lastname').value,
            email: document.getElementById('customer-email').value
        });
        const response = await fetch(`${API_URL}/customers`, { method: 'POST', headers: commonHeaders, body });
        if (response.ok) {
            showStatus('Customer created successfully!');
            customerForm.reset();
            refreshCustomers();
        } else {
            showStatus(`Error creating customer: ${response.statusText}`, true);
        }
    });

    productForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const body = JSON.stringify({
            name: document.getElementById('product-name').value,
            description: document.getElementById('product-description').value,
            price: parseFloat(document.getElementById('product-price').value),
            stock: parseInt(document.getElementById('product-quantity').value)
        });
        const response = await fetch(`${API_URL}/products`, { method: 'POST', headers: commonHeaders, body });
        if (response.ok) {
            showStatus('Product created successfully!');
            productForm.reset();
            refreshProducts();
        } else {
            showStatus(`Error creating product: ${response.statusText}`, true);
        }
    });

    orderForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const itemRows = orderItemsContainer.querySelectorAll('.order-item-row');
        if (itemRows.length === 0) {
            showStatus('Please add at least one product to the order.', true);
            return;
        }

        const orderItems = Array.from(itemRows).map(row => ({
            productId: parseInt(row.querySelector('.order-product-select').value),
            quantity: parseInt(row.querySelector('.order-quantity-input').value)
        }));

        const body = JSON.stringify({
            customerId: parseInt(orderCustomerSelect.value),
            orderItems: orderItems
        });

        const response = await fetch(`${API_URL}/orders`, { method: 'POST', headers: commonHeaders, body });
        if (response.ok) {
            showStatus('Order placed successfully!');
            orderItemsContainer.innerHTML = '';
            addOrderItemRow();
            refreshOrders();
            refreshProducts(); // To show updated stock
        } else {
            const errorData = await response.json();
            showStatus(`Error placing order: ${errorData.message || response.statusText}`, true);
        }
    });

    // --- Initial Load ---
    async function initialLoad() {
        await refreshCustomers();
        await refreshProducts();
        await refreshOrders();
        addOrderItemRow(); // Add the first empty row for a new order
    }

    initialLoad();
});
