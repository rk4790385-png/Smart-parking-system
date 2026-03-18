const API_BASE_URL = 'http://localhost:8080/api';

// --- View Router Elements ---
const authView = document.getElementById('authView');
const aboutView = document.getElementById('aboutView');
const dashboardView = document.getElementById('dashboardView');

const navAboutBtn = document.getElementById('navAboutBtn');
const navDashboardBtn = document.getElementById('navDashboardBtn');
const navLogoutBtn = document.getElementById('navLogoutBtn');
const backToHomeBtn = document.getElementById('backToHomeBtn');

// --- Auth Elements ---
const usernameInput = document.getElementById('usernameInput');
const passwordInput = document.getElementById('passwordInput');
const loginBtn = document.getElementById('loginBtn');
const registerBtn = document.getElementById('registerBtn');
const authMessage = document.getElementById('authMessage');
const currentUserDisp = document.getElementById('currentUserDisp');

// --- Dashboard Elements ---
const parkingGrid = document.getElementById('parkingGrid');
const slotInput = document.getElementById('slotInput');
const licensePlateInput = document.getElementById('licensePlateInput');
const parkBtn = document.getElementById('parkBtn');
const unparkBtn = document.getElementById('unparkBtn');
const messageBox = document.getElementById('messageBox');
const vehiclesTbody = document.getElementById('vehiclesTbody');

const statTotal = document.getElementById('stat-total');
const statOccupied = document.getElementById('stat-occupied');
const statAvailable = document.getElementById('stat-available');

const receiptModal = document.getElementById('receiptModal');
const closeModalBtn = document.getElementById('closeModalBtn');

// Base config
const ROW_LETTERS = ['A', 'B', 'C', 'D', 'E'];
const COLS_PER_ROW = 10;
let currentOccupiedMap = {}; 
let refreshInterval = null;

// --- STATE MANAGER ---
function getToken() { return localStorage.getItem('ozone_token'); }
function getAuthHeaders() { return { 'Authorization': `Bearer ${getToken()}` }; }

document.addEventListener('DOMContentLoaded', () => {
    initGrid();
    checkAuthStatus();
});

function checkAuthStatus() {
    if (getToken()) {
        currentUserDisp.innerText = localStorage.getItem('ozone_user');
        switchView('dashboard');
    } else {
        switchView('auth');
    }
}

function switchView(viewName) {
    authView.classList.add('hidden');
    aboutView.classList.add('hidden');
    dashboardView.classList.add('hidden');
    
    // Reset Nav logic
    navDashboardBtn.classList.remove('hidden');
    navLogoutBtn.classList.remove('hidden');

    if (viewName === 'auth') {
        authView.classList.remove('hidden');
        navDashboardBtn.classList.add('hidden');
        navLogoutBtn.classList.add('hidden');
        if(refreshInterval) clearInterval(refreshInterval);
    } else if (viewName === 'about') {
        aboutView.classList.remove('hidden');
        if(!getToken()) {
            navDashboardBtn.classList.add('hidden');
            navLogoutBtn.classList.add('hidden');
        }
    } else if (viewName === 'dashboard') {
        dashboardView.classList.remove('hidden');
        refreshDashboard();
        if(!refreshInterval) refreshInterval = setInterval(refreshDashboard, 5000);
    }
}

// Nav Listeners
navAboutBtn.onclick = () => switchView('about');
navDashboardBtn.onclick = () => switchView('dashboard');
backToHomeBtn.onclick = () => checkAuthStatus();
navLogoutBtn.onclick = async () => {
    try {
        await fetch(`${API_BASE_URL}/auth/logout`, { method: 'POST', headers: getAuthHeaders() });
    } catch(e) {}
    localStorage.removeItem('ozone_token');
    localStorage.removeItem('ozone_user');
    checkAuthStatus();
};

// --- AUTH LOGIC ---
const handleAuth = async (endpoint) => {
    const user = usernameInput.value.trim();
    const pass = passwordInput.value.trim();
    if (!user || !pass) {
        showAuthMessage('Please enter both username and password!', 'error');
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/auth/${endpoint}?username=${user}&password=${pass}`, { method: 'POST' });
        const data = await response.json();
        
        if (response.ok) {
            localStorage.setItem('ozone_token', data.token);
            localStorage.setItem('ozone_user', data.username);
            usernameInput.value = '';
            passwordInput.value = '';
            checkAuthStatus(); // Route to dashboard
        } else {
            showAuthMessage(data.error || 'Authentication Failed', 'error');
        }
    } catch (e) { showAuthMessage('Server unreachable.', 'error'); }
};

loginBtn.onclick = () => handleAuth('login');
registerBtn.onclick = () => handleAuth('register');

function showAuthMessage(msg, type) {
    authMessage.innerText = msg;
    authMessage.className = `message ${type}`;
    authMessage.classList.remove('hidden');
    setTimeout(() => authMessage.classList.add('hidden'), 5000);
}

// --- DASHBOARD LOGIC ---
function initGrid() {
    parkingGrid.innerHTML = '';
    for (let i = 0; i < ROW_LETTERS.length; i++) {
        for (let j = 1; j <= COLS_PER_ROW; j++) {
            const slotId = `${ROW_LETTERS[i]}${j}`;
            const div = document.createElement('div');
            div.className = 'slot';
            div.id = `slot-${slotId}`;
            div.innerText = slotId;
            div.onclick = () => selectSlot(slotId);
            parkingGrid.appendChild(div);
        }
    }
}

async function refreshDashboard() {
    try {
        const response = await fetch(`${API_BASE_URL}/parking/status`, { headers: getAuthHeaders() });
        if (response.status === 401) {
            // Token expired or invalid
            localStorage.removeItem('ozone_token');
            checkAuthStatus();
            return;
        }
        if (!response.ok) throw new Error('API Error');
        const data = await response.json();
        
        statTotal.innerText = data.totalSlots;
        statOccupied.innerText = data.occupiedSlots;
        statAvailable.innerText = data.availableSlots;
        
        document.querySelectorAll('.slot').forEach(el => el.className = 'slot'); // reset to empty
        currentOccupiedMap = {};
        data.activeSlots.forEach(v => {
            currentOccupiedMap[v.slotNumber.toUpperCase()] = v;
            const slotEl = document.getElementById(`slot-${v.slotNumber.toUpperCase()}`);
            if(slotEl) slotEl.classList.add(`occupied-${v.vehicleType}`);
        });
        renderTable(data.activeSlots);
    } catch (e) { console.error('Dashboard Auth Blocked or Offline'); }
}

function renderTable(activeSlots) {
    vehiclesTbody.innerHTML = '';
    activeSlots.sort((a,b) => new Date(b.entryTime) - new Date(a.entryTime));
    activeSlots.forEach(v => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${v.slotNumber}</strong></td>
            <td>${v.licensePlate}</td>
            <td><span class="badge ${v.vehicleType.toLowerCase()}">${v.vehicleType}</span></td>
            <td>${new Date(v.entryTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</td>
            <td><button class="btn-small" onclick="processUnpark('${v.licensePlate}')">Checkout</button></td>
        `;
        vehiclesTbody.appendChild(tr);
    });
}

function selectSlot(slotId) {
    if (currentOccupiedMap[slotId]) {
        licensePlateInput.value = currentOccupiedMap[slotId].licensePlate;
        slotInput.value = slotId;
    } else {
        document.querySelectorAll('.slot').forEach(el => el.classList.remove('selected-slot'));
        document.getElementById(`slot-${slotId}`).classList.add('selected-slot');
        slotInput.value = slotId;
    }
}

parkBtn.addEventListener('click', async () => {
    const plate = licensePlateInput.value.trim();
    const slot = slotInput.value.trim();
    const vType = document.querySelector('input[name="vType"]:checked').value;
    
    if (!plate || !slot) {
        showDashMessage('Fill License Plate and pick a Grid slot.', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/parking/park?licensePlate=${plate}&vehicleType=${vType}&slotNumber=${slot}`, { 
            method: 'POST', headers: getAuthHeaders() 
        });
        const data = await response.json();
        
        if (response.ok) {
            showDashMessage(`Success!! Auth Entry for ${plate}.`, 'success');
            licensePlateInput.value = ''; slotInput.value = '';
            refreshDashboard();
        } else {
            showDashMessage(data.error || 'Park Error', 'error');
        }
    } catch (e) { showDashMessage('Network error.', 'error'); }
});

unparkBtn.addEventListener('click', () => {
    const plate = licensePlateInput.value.trim();
    if (!plate) return showDashMessage('Provide plate to checkout.', 'error');
    processUnpark(plate);
});

window.processUnpark = async function(plate) {
    try {
        const response = await fetch(`${API_BASE_URL}/parking/unpark?licensePlate=${plate}`, {
             method: 'POST', headers: getAuthHeaders() 
        });
        const data = await response.json();
        
        if (response.ok) {
            licensePlateInput.value = ''; slotInput.value = '';
            refreshDashboard();
            showReceipt(data);
        } else {
            showDashMessage(data.error || 'Unpark Error', 'error');
        }
    } catch (e) { showDashMessage('Fatal Error.', 'error'); }
};

function showReceipt(vehicleData) {
    document.getElementById('r-plate').innerText = vehicleData.licensePlate;
    document.getElementById('r-type').innerText = vehicleData.vehicleType;
    document.getElementById('r-entry').innerText = new Date(vehicleData.entryTime).toLocaleString();
    document.getElementById('r-exit').innerText = new Date(vehicleData.exitTime).toLocaleString();
    document.getElementById('r-fee').innerText = vehicleData.fee.toFixed(2);
    receiptModal.classList.remove('hidden');
}

closeModalBtn.onclick = () => receiptModal.classList.add('hidden');

function showDashMessage(msg, type) {
    messageBox.innerText = msg;
    messageBox.className = `message ${type}`;
    messageBox.classList.remove('hidden');
    setTimeout(() => messageBox.classList.add('hidden'), 5000);
}
