import StatusChip, { StatusVariant } from '@/components/ui/status-chip';

interface PaymentStatusBadgeProps {
  status: string;
  className?: string;
}

const PAYMENT_STATUS_MAP: Record<string, { label: string; variant: StatusVariant }> = {
  CREATED: { label: 'Pending', variant: 'WARNING' },
  PENDING: { label: 'Pending', variant: 'WARNING' },
  PAID: { label: 'Paid', variant: 'SUCCESS' },
  FAILED: { label: 'Failed', variant: 'DANGER' },
  REFUNDED: { label: 'Refunded', variant: 'INFO' },
};

export function PaymentStatusBadge({ status, className }: PaymentStatusBadgeProps) {
  const normalizedStatus = status?.toUpperCase() || 'UNKNOWN';
  const mappedStatus = PAYMENT_STATUS_MAP[normalizedStatus] || {
    label: normalizedStatus,
    variant: 'INFO' as StatusVariant,
  };

  return (
    <StatusChip 
      status={mappedStatus.variant} 
      label={mappedStatus.label} 
      className={className} 
    />
  );
}
