'use strict';

/**
 * Front-end controller of the address book.
 *
 * The page talks to the same Spring Boot application that serves it: every piece of state lives in
 * the SQLite database behind the REST API, so a reload never loses data. The script only renders
 * what the API returns, which keeps the UI and the stored data consistent.
 */

const API = {
    contacts: '/api/contacts',
    summary: '/api/contacts/summary',
    exportUrl: '/api/excel/export',
    templateUrl: '/api/excel/template',
    importUrl: '/api/excel/import'
};

const METHOD_TYPES = [
    {value: 'PHONE', label: '电话', icon: '☎'},
    {value: 'EMAIL', label: '邮箱', icon: '✉'},
    {value: 'SOCIAL', label: '社交', icon: '@'},
    {value: 'ADDRESS', label: '地址', icon: '⌖'}
];

const state = {
    contacts: [],
    summary: {total: 0, bookmarked: 0, groups: []},
    filter: {type: 'all', group: null},
    keyword: '',
    view: 'cards',
    editingId: null
};

const els = {
    search: document.getElementById('searchInput'),
    countAll: document.getElementById('countAll'),
    countBookmarked: document.getElementById('countBookmarked'),
    groupList: document.getElementById('groupList'),
    listTitle: document.getElementById('listTitle'),
    listSubtitle: document.getElementById('listSubtitle'),
    cardGrid: document.getElementById('cardGrid'),
    tableView: document.getElementById('tableView'),
    tableBody: document.getElementById('tableBody'),
    emptyState: document.getElementById('emptyState'),
    emptyTitle: document.getElementById('emptyTitle'),
    emptyText: document.getElementById('emptyText'),
    viewCards: document.getElementById('viewCards'),
    viewTable: document.getElementById('viewTable'),
    modal: document.getElementById('modal'),
    modalTitle: document.getElementById('modalTitle'),
    contactForm: document.getElementById('contactForm'),
    fieldName: document.getElementById('fieldName'),
    fieldGroup: document.getElementById('fieldGroup'),
    fieldCompany: document.getElementById('fieldCompany'),
    fieldTitle: document.getElementById('fieldTitle'),
    fieldNotes: document.getElementById('fieldNotes'),
    fieldBookmarked: document.getElementById('fieldBookmarked'),
    methodRows: document.getElementById('methodRows'),
    methodsHint: document.getElementById('methodsHint'),
    groupOptions: document.getElementById('groupOptions'),
    confirmModal: document.getElementById('confirmModal'),
    confirmText: document.getElementById('confirmText'),
    messageModal: document.getElementById('messageModal'),
    messageTitle: document.getElementById('messageTitle'),
    messageText: document.getElementById('messageText'),
    messageList: document.getElementById('messageList'),
    toast: document.getElementById('toast'),
    importInput: document.getElementById('importInput')
};

/* ------------------------------------------------------------------ helpers */

function escapeHtml(value) {
    return String(value === null || value === undefined ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function avatarStyle(name) {
    let hash = 0;
    for (const character of String(name)) {
        hash = (hash * 31 + character.codePointAt(0)) % 360;
    }
    return 'background: linear-gradient(140deg, hsl(' + hash + ' 72% 58%), hsl('
        + ((hash + 32) % 360) + ' 66% 44%))';
}

function firstCharacter(name) {
    const text = String(name || '?').trim();
    return text ? Array.from(text)[0].toUpperCase() : '?';
}

function methodMeta(type) {
    return METHOD_TYPES.find(item => item.value === type) || {value: type, label: type, icon: '•'};
}

function formatDate(value) {
    if (!value) {
        return '—';
    }
    const text = String(value);
    return text.length >= 10 ? text.substring(5, 10).replace('-', '/') : text;
}

function showToast(message, kind) {
    els.toast.textContent = message;
    els.toast.classList.remove('is-hidden');
    els.toast.classList.toggle('is-visible', true);
    els.toast.classList.toggle('is-error', kind === 'error');
    els.toast.classList.toggle('is-success', kind === 'success');
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => {
        els.toast.classList.remove('is-visible');
        window.setTimeout(() => els.toast.classList.add('is-hidden'), 220);
    }, 3200);
}

async function api(path, options) {
    const response = await fetch(path, options);
    if (!response.ok) {
        let message = '请求失败（HTTP ' + response.status + '）';
        try {
            const body = await response.json();
            if (body && body.errors && body.errors.length) {
                message = body.errors.join('；');
            } else if (body && body.message) {
                message = body.message;
            }
        } catch (ignored) {
            // keep the generic message
        }
        throw new Error(message);
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function download(url) {
    const link = document.createElement('a');
    link.href = url;
    link.rel = 'noopener';
    document.body.appendChild(link);
    link.click();
    link.remove();
}

/* ------------------------------------------------------------------ loading */

async function load() {
    const params = new URLSearchParams();
    if (state.keyword) {
        params.set('q', state.keyword);
    }
    if (state.filter.type === 'bookmarked') {
        params.set('bookmarked', 'true');
    }
    if (state.filter.type === 'group' && state.filter.group) {
        params.set('group', state.filter.group);
    }
    const query = params.toString();
    try {
        const results = await Promise.all([
            api(API.contacts + (query ? '?' + query : '')),
            api(API.summary)
        ]);
        state.contacts = results[0];
        state.summary = results[1];
        render();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

/* ------------------------------------------------------------------ render */

function render() {
    els.countAll.textContent = state.summary.total;
    els.countBookmarked.textContent = state.summary.bookmarked;
    renderGroups();
    renderTitle();
    renderGroupOptions();

    const hasContacts = state.contacts.length > 0;
    els.emptyState.classList.toggle('is-hidden', hasContacts);
    els.cardGrid.classList.toggle('is-hidden', !hasContacts || state.view !== 'cards');
    els.tableView.classList.toggle('is-hidden', !hasContacts || state.view !== 'table');

    if (!hasContacts) {
        renderEmpty();
        return;
    }
    if (state.view === 'cards') {
        els.cardGrid.innerHTML = state.contacts.map(cardTemplate).join('');
    } else {
        els.tableBody.innerHTML = state.contacts.map(rowTemplate).join('');
    }
}

function renderTitle() {
    if (state.filter.type === 'bookmarked') {
        els.listTitle.textContent = '收藏的联系人';
    } else if (state.filter.type === 'group') {
        els.listTitle.textContent = '分组：' + state.filter.group;
    } else {
        els.listTitle.textContent = '全部联系人';
    }
    let subtitle = '共 ' + state.contacts.length + ' 位联系人';
    if (state.keyword) {
        subtitle += '，关键词「' + state.keyword + '」';
    }
    els.listSubtitle.textContent = subtitle;
}

function renderEmpty() {
    if (state.keyword) {
        els.emptyTitle.textContent = '没有匹配的联系人';
        els.emptyText.textContent = '换个关键词试试，或者清空搜索框。';
        return;
    }
    if (state.filter.type === 'bookmarked') {
        els.emptyTitle.textContent = '还没有收藏的联系人';
        els.emptyText.textContent = '在联系人卡片右上角点一下星标，就会出现在这里。';
        return;
    }
    if (state.filter.type === 'group') {
        els.emptyTitle.textContent = '这个分组还没有联系人';
        els.emptyText.textContent = '新增联系人时填上分组，或切换到「全部联系人」。';
        return;
    }
    els.emptyTitle.textContent = '还没有联系人';
    els.emptyText.textContent = '点击右上角「新增联系人」，或先下载模板批量导入。';
}

function renderGroups() {
    const groups = state.summary.groups || [];
    if (!groups.length) {
        els.groupList.innerHTML = '<p class="methods-hint" style="padding:6px 12px">暂无分组</p>';
        return;
    }
    els.groupList.innerHTML = groups.map(group => {
        const active = state.filter.type === 'group' && state.filter.group === group.name;
        return '<button class="group-item' + (active ? ' is-active' : '')
            + '" data-group="' + escapeHtml(group.name) + '" data-testid="group-' + escapeHtml(group.name) + '">'
            + '<span class="dot"></span><span>' + escapeHtml(group.name) + '</span>'
            + '<span class="count">' + group.count + '</span></button>';
    }).join('');
}

function renderGroupOptions() {
    els.groupOptions.innerHTML = (state.summary.groups || [])
        .map(group => '<option value="' + escapeHtml(group.name) + '"></option>')
        .join('');
}

function methodsTemplate(methods) {
    if (!methods || !methods.length) {
        return '<li class="method-empty">还没有联系方式</li>';
    }
    return methods.map(method => {
        const meta = methodMeta(method.type);
        return '<li class="method">'
            + '<span class="m-icon">' + meta.icon + '</span>'
            + (method.label ? '<span class="m-label">' + escapeHtml(method.label) + '</span>' : '')
            + '<span class="m-value">' + escapeHtml(method.value) + '</span>'
            + '</li>';
    }).join('');
}

function cardTemplate(contact) {
    const groupBadge = contact.group
        ? '<span class="group-badge">' + escapeHtml(contact.group) + '</span>'
        : '';
    const subtitle = [contact.company, contact.title].filter(Boolean).join(' · ') || '未填写公司 / 职位';
    return '<article class="card' + (contact.bookmarked ? ' is-bookmarked' : '') + '"'
        + ' data-id="' + contact.id + '" data-testid="card-' + contact.id + '">'
        + '<div class="card-head">'
        + '<div class="avatar" style="' + avatarStyle(contact.name) + '">'
        + escapeHtml(firstCharacter(contact.name)) + '</div>'
        + '<div class="card-identity">'
        + '<h3 class="card-name">' + escapeHtml(contact.name) + groupBadge + '</h3>'
        + '<p class="card-sub">' + escapeHtml(subtitle) + '</p>'
        + '</div>'
        + '<button class="star-btn' + (contact.bookmarked ? ' is-on' : '') + '"'
        + ' data-action="bookmark" data-id="' + contact.id + '"'
        + ' title="' + (contact.bookmarked ? '取消收藏' : '加入收藏') + '"'
        + ' aria-label="收藏">' + (contact.bookmarked ? '★' : '☆') + '</button>'
        + '</div>'
        + '<ul class="methods">' + methodsTemplate(contact.methods) + '</ul>'
        + (contact.notes ? '<p class="card-notes">' + escapeHtml(contact.notes) + '</p>' : '')
        + '<div class="card-foot">'
        + '<button class="btn-link" data-action="edit" data-id="' + contact.id + '">编辑</button>'
        + '<button class="btn-link danger" data-action="delete" data-id="' + contact.id + '">删除</button>'
        + '<span class="spacer"></span>'
        + '<span class="card-updated">更新于 ' + formatDate(contact.updatedAt) + '</span>'
        + '</div>'
        + '</article>';
}

function rowTemplate(contact) {
    const subtitle = [contact.company, contact.title].filter(Boolean).join(' · ');
    const pills = (contact.methods || []).map(method =>
        '<span class="pill is-' + method.type.toLowerCase() + '">'
        + (method.label ? escapeHtml(method.label) + '：' : '')
        + escapeHtml(method.value) + '</span>').join('');
    return '<tr data-id="' + contact.id + '">'
        + '<td><div class="cell-name"><span class="avatar" style="' + avatarStyle(contact.name) + '">'
        + escapeHtml(firstCharacter(contact.name)) + '</span>' + escapeHtml(contact.name) + '</div></td>'
        + '<td>' + (contact.group ? '<span class="group-badge">' + escapeHtml(contact.group) + '</span>' : '—') + '</td>'
        + '<td>' + (subtitle ? escapeHtml(subtitle) : '—') + '</td>'
        + '<td><div class="cell-methods">' + (pills || '—') + '</div></td>'
        + '<td><button class="star-btn' + (contact.bookmarked ? ' is-on' : '') + '"'
        + ' data-action="bookmark" data-id="' + contact.id + '" aria-label="收藏">'
        + (contact.bookmarked ? '★' : '☆') + '</button></td>'
        + '<td><div class="row-actions">'
        + '<button class="btn-link" data-action="edit" data-id="' + contact.id + '">编辑</button>'
        + '<button class="btn-link danger" data-action="delete" data-id="' + contact.id + '">删除</button>'
        + '</div></td>'
        + '</tr>';
}

/* ------------------------------------------------------------------ editing */

function addMethodRow(method) {
    const meta = methodMeta(method && method.type ? method.type : 'PHONE');
    const row = document.createElement('div');
    row.className = 'method-row';
    row.innerHTML =
        '<select class="m-type" aria-label="联系方式类型">'
        + METHOD_TYPES.map(type => '<option value="' + type.value + '"'
            + (type.value === meta.value ? ' selected' : '') + '>' + type.label + '</option>').join('')
        + '</select>'
        + '<input class="m-label-input" maxlength="50" placeholder="标签（可选）"'
        + ' value="' + escapeHtml(method && method.label ? method.label : '') + '">'
        + '<input class="m-value" maxlength="300" placeholder="号码 / 邮箱 / 账号 / 地址"'
        + ' value="' + escapeHtml(method && method.value ? method.value : '') + '">'
        + '<button type="button" class="remove-method" title="移除">✕</button>';
    els.methodRows.appendChild(row);
    updateMethodsHint();
    return row;
}

function updateMethodsHint() {
    els.methodsHint.classList.toggle('is-hidden', els.methodRows.children.length > 0);
}

function openEditor(contact) {
    state.editingId = contact ? contact.id : null;
    els.modalTitle.textContent = contact ? '编辑联系人' : '新增联系人';
    els.fieldName.value = contact ? contact.name : '';
    els.fieldGroup.value = contact ? (contact.group || '') : '';
    els.fieldCompany.value = contact ? (contact.company || '') : '';
    els.fieldTitle.value = contact ? (contact.title || '') : '';
    els.fieldNotes.value = contact ? (contact.notes || '') : '';
    els.fieldBookmarked.checked = contact ? !!contact.bookmarked : false;
    els.methodRows.innerHTML = '';
    (contact && contact.methods ? contact.methods : []).forEach(addMethodRow);
    updateMethodsHint();

    els.modal.classList.remove('is-hidden');
    window.setTimeout(() => els.fieldName.focus(), 30);
}

function closeEditor() {
    els.modal.classList.add('is-hidden');
    state.editingId = null;
}

function collectPayload() {
    const methods = Array.from(els.methodRows.querySelectorAll('.method-row'))
        .map(row => ({
            type: row.querySelector('.m-type').value,
            label: row.querySelector('.m-label-input').value.trim(),
            value: row.querySelector('.m-value').value.trim()
        }))
        .filter(method => method.value);
    return {
        name: els.fieldName.value.trim(),
        group: els.fieldGroup.value.trim(),
        company: els.fieldCompany.value.trim(),
        title: els.fieldTitle.value.trim(),
        notes: els.fieldNotes.value.trim(),
        bookmarked: els.fieldBookmarked.checked,
        methods: methods
    };
}

async function saveContact(event) {
    event.preventDefault();
    const payload = collectPayload();
    if (!payload.name) {
        showToast('姓名不能为空', 'error');
        els.fieldName.focus();
        return;
    }
    const editing = state.editingId !== null;
    const button = document.getElementById('saveBtn');
    button.disabled = true;
    try {
        await api(editing ? API.contacts + '/' + state.editingId : API.contacts, {
            method: editing ? 'PUT' : 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        closeEditor();
        showToast(editing ? '已保存修改' : '已添加联系人', 'success');
        await load();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        button.disabled = false;
    }
}

function askDelete(contact) {
    els.confirmText.textContent = '确定要删除「' + contact.name + '」吗？该操作不可撤销。';
    els.confirmModal.classList.remove('is-hidden');
    els.confirmModal.dataset.id = contact.id;
}

async function confirmDelete() {
    const id = els.confirmModal.dataset.id;
    els.confirmModal.classList.add('is-hidden');
    try {
        await api(API.contacts + '/' + id, {method: 'DELETE'});
        showToast('已删除联系人', 'success');
        await load();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function toggleBookmark(contact) {
    try {
        await api(API.contacts + '/' + contact.id + '/bookmark', {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({bookmarked: !contact.bookmarked})
        });
        await load();
        showToast(contact.bookmarked ? '已取消收藏' : '已加入收藏', 'success');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

/* ------------------------------------------------------------------ import / export */

async function importFile(file) {
    const form = new FormData();
    form.append('file', file);
    const button = document.getElementById('importBtn');
    button.disabled = true;
    try {
        const result = await api(API.importUrl, {method: 'POST', body: form});
        await load();
        if (result.failed > 0) {
            els.messageTitle.textContent = '导入完成，有 ' + result.failed + ' 行未导入';
            els.messageText.textContent = '成功导入 ' + result.imported + ' 条，失败 ' + result.failed + ' 条。';
            els.messageList.innerHTML = (result.errors || [])
                .map(error => '<div>· ' + escapeHtml(error) + '</div>').join('');
            els.messageModal.classList.remove('is-hidden');
        } else {
            showToast('成功导入 ' + result.imported + ' 位联系人', 'success');
        }
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        button.disabled = false;
        els.importInput.value = '';
    }
}

/* ------------------------------------------------------------------ events */

function bindEvents() {
    document.getElementById('addBtn').addEventListener('click', () => openEditor(null));
    document.getElementById('emptyAddBtn').addEventListener('click', () => openEditor(null));
    document.getElementById('modalClose').addEventListener('click', closeEditor);
    document.getElementById('cancelBtn').addEventListener('click', closeEditor);
    els.contactForm.addEventListener('submit', saveContact);

    document.getElementById('confirmCancel').addEventListener('click', () => {
        els.confirmModal.classList.add('is-hidden');
    });
    document.getElementById('confirmDelete').addEventListener('click', confirmDelete);
    document.getElementById('messageClose').addEventListener('click', () => {
        els.messageModal.classList.add('is-hidden');
    });

    document.querySelectorAll('[data-add]').forEach(button => {
        button.addEventListener('click', () => addMethodRow({type: button.dataset.add}));
    });

    els.methodRows.addEventListener('click', event => {
        const remove = event.target.closest('.remove-method');
        if (remove) {
            remove.closest('.method-row').remove();
            updateMethodsHint();
        }
    });

    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', () => {
            document.querySelectorAll('.nav-item').forEach(other => other.classList.remove('is-active'));
            item.classList.add('is-active');
            state.filter = {type: item.dataset.filter, group: null};
            load();
        });
    });

    els.groupList.addEventListener('click', event => {
        const item = event.target.closest('.group-item');
        if (!item) {
            return;
        }
        document.querySelectorAll('.nav-item').forEach(other => other.classList.remove('is-active'));
        state.filter = {type: 'group', group: item.dataset.group};
        load();
    });

    els.cardGrid.addEventListener('click', handleCardAction);
    els.tableBody.addEventListener('click', handleCardAction);

    els.search.addEventListener('input', () => {
        window.clearTimeout(bindEvents.searchTimer);
        bindEvents.searchTimer = window.setTimeout(() => {
            state.keyword = els.search.value.trim();
            load();
        }, 250);
    });

    els.viewCards.addEventListener('click', () => setView('cards'));
    els.viewTable.addEventListener('click', () => setView('table'));

    document.getElementById('exportBtn').addEventListener('click', () => {
        download(API.exportUrl);
        showToast('正在导出 Excel…', 'success');
    });
    document.getElementById('templateBtn').addEventListener('click', () => {
        download(API.templateUrl);
        showToast('正在下载导入模板…', 'success');
    });
    document.getElementById('importBtn').addEventListener('click', () => els.importInput.click());
    els.importInput.addEventListener('change', () => {
        if (els.importInput.files && els.importInput.files[0]) {
            importFile(els.importInput.files[0]);
        }
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            els.modal.classList.add('is-hidden');
            els.confirmModal.classList.add('is-hidden');
            els.messageModal.classList.add('is-hidden');
        }
    });
}

function handleCardAction(event) {
    const button = event.target.closest('[data-action]');
    if (!button) {
        return;
    }
    const id = Number(button.dataset.id);
    const contact = state.contacts.find(item => item.id === id);
    if (!contact) {
        return;
    }
    if (button.dataset.action === 'bookmark') {
        toggleBookmark(contact);
    } else if (button.dataset.action === 'edit') {
        openEditor(contact);
    } else if (button.dataset.action === 'delete') {
        askDelete(contact);
    }
}

function setView(view) {
    state.view = view;
    els.viewCards.classList.toggle('is-active', view === 'cards');
    els.viewTable.classList.toggle('is-active', view === 'table');
    render();
}

bindEvents();
load();
