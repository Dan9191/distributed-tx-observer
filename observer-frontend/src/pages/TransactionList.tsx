import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getTransactions } from '../api'

/**
 * Список зарегистрированных транзакций.
 * Каждая строка — кнопки перехода в редактор шаблона и в визуализатор.
 */
export default function TransactionList() {
  const [transactions, setTransactions] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    getTransactions()
      .then(setTransactions)
      .catch(() => setError('Не удалось загрузить список транзакций'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="max-w-3xl mx-auto p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-900">Distributed TX Monitor</h1>
        <p className="text-gray-500 mt-1">Визуализация распределённых транзакций</p>
      </div>

      {loading && <p className="text-gray-400">Загрузка...</p>}

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">{error}</div>
      )}

      {!loading && !error && transactions.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <p className="text-lg">Нет зарегистрированных транзакций</p>
          <p className="text-sm mt-1">Запустите demo-service — шаги зарегистрируются автоматически</p>
        </div>
      )}

      <div className="space-y-3">
        {transactions.map(name => (
          <div
            key={name}
            className="bg-white border border-gray-200 rounded-lg p-5
                       flex items-center justify-between
                       hover:border-blue-300 hover:shadow-sm transition-all"
          >
            <p className="font-medium text-gray-900">{name}</p>
            <div className="flex gap-2">
              <button
                onClick={() => navigate(`/transactions/${encodeURIComponent(name)}/template`)}
                className="px-4 py-2 text-sm rounded-md border border-gray-200
                           text-gray-600 hover:bg-gray-50 transition-colors"
              >
                Шаблон
              </button>
              <button
                onClick={() => navigate(`/transactions/${encodeURIComponent(name)}/visualize`)}
                className="px-4 py-2 text-sm rounded-md bg-blue-600 text-white
                           hover:bg-blue-700 transition-colors"
              >
                Визуализация
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
